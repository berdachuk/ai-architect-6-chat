# Design
## Detailed Design (schema, services, MCP, harness, frontend)

**Version:** 1.0.0
**Requirements:** [01-requirements.md](01-requirements.md) · [02-architecture.md](02-architecture.md)

---

## Database Schema

### Flyway Migration `V1__init_chat_schema.sql`

```sql
CREATE SCHEMA IF NOT EXISTS ai_chat;

-- Chat sessions
CREATE TABLE ai_chat.chat (
    id               CHAR(24)    PRIMARY KEY,
    user_id          VARCHAR(255) NOT NULL,
    name             VARCHAR(255),
    agent_id         VARCHAR(50)  DEFAULT 'auto',
    is_default       BOOLEAN      DEFAULT FALSE,
    created_at       TIMESTAMPTZ  DEFAULT now(),
    updated_at       TIMESTAMPTZ  DEFAULT now(),
    last_activity_at TIMESTAMPTZ  DEFAULT now(),
    message_count    INT          DEFAULT 0
);

CREATE UNIQUE INDEX idx_chat_user_default
    ON ai_chat.chat (user_id, is_default) WHERE is_default = TRUE;

CREATE INDEX idx_chat_user_activity
    ON ai_chat.chat (user_id, last_activity_at DESC);

-- Chat messages
CREATE TABLE ai_chat.chat_message (
    id              CHAR(24)    PRIMARY KEY,
    chat_id         CHAR(24)    NOT NULL REFERENCES ai_chat.chat(id) ON DELETE CASCADE,
    role            VARCHAR(20)  NOT NULL CHECK (role IN ('user', 'assistant', 'system')),
    content         TEXT         NOT NULL,
    sequence_number INT          NOT NULL,
    tokens_used     INT,
    created_at      TIMESTAMPTZ  DEFAULT now(),
    metadata        JSONB,
    deleted_at      TIMESTAMPTZ
);

CREATE UNIQUE INDEX idx_chat_message_seq
    ON ai_chat.chat_message (chat_id, sequence_number);

CREATE INDEX idx_chat_message_chat
    ON ai_chat.chat_message (chat_id, created_at);

-- Spring AI Session JDBC tables (auto-created by spring-ai-starter-session-jdbc)
-- ai_session: session_id, created_at, updated_at, metadata
-- ai_session_event: event_id, session_id, message_type, content, metadata, created_at
```

### ID Generation

```java
// core/util/IdGenerator.java
public final class IdGenerator {
    private static final SecureRandom RANDOM = new SecureRandom();

    public static String generateId() {
        byte[] bytes = new byte[12];
        RANDOM.nextBytes(bytes);
        // Set timestamp bytes (first 4) to current time seconds for sortability
        int timestamp = (int) (System.currentTimeMillis() / 1000);
        bytes[0] = (byte) (timestamp >>> 24);
        bytes[1] = (byte) (timestamp >>> 16);
        bytes[2] = (byte) (timestamp >>> 8);
        bytes[3] = (byte) timestamp;
        return HexFormat.of().formatHex(bytes);
    }
}
```

---

## Domain Records (`chat/domain`)

```java
// chat/domain/Chat.java
public record Chat(
    String   id,
    String   userId,
    String   name,
    String   agentId,
    boolean  isDefault,
    Instant  createdAt,
    Instant  updatedAt,
    Instant  lastActivityAt,
    int      messageCount
) {}

// chat/domain/ChatMessage.java
public record ChatMessage(
    String   id,
    String   chatId,
    String   role,
    String   content,
    int      sequenceNumber,
    Integer  tokensUsed,
    Instant  createdAt,
    Map<String, Object> metadata
) {
    public static final String ROLE_USER = "user";
    public static final String ROLE_ASSISTANT = "assistant";
    public static final String ROLE_SYSTEM = "system";
}
```

---

## Repository Layer (`chat/repository`)

### ChatRepository

```java
// chat/repository/ChatRepository.java
public interface ChatRepository {
    Optional<Chat> findById(String id);
    List<Chat> findByUserId(String userId);
    Optional<Chat> findDefaultByUserId(String userId);
    Chat insert(Chat chat);
    void updateName(String id, String name);
    void updateActivity(String id, Instant lastActivityAt, int messageCount);
    void deleteById(String id);
    boolean existsByUserId(String userId);
}
```

### ChatMessageRepository

```java
// chat/repository/ChatMessageRepository.java
public interface ChatMessageRepository {
    List<ChatMessage> findByChatId(String chatId, int limit, int offset);
    ChatMessage insert(ChatMessage message);
    int getNextSequenceNumber(String chatId);
    void softDeleteByChatId(String chatId);
    int countByChatId(String chatId);
}
```

### JDBC Implementation Pattern

```java
// chat/repository/impl/ChatRepositoryImpl.java
@Repository
public class ChatRepositoryImpl implements ChatRepository {
    private final NamedParameterJdbcTemplate jdbc;

    @InjectSql("classpath:sql/chat/insert.sql")
    private String insertSql;

    @InjectSql("classpath:sql/chat/selectById.sql")
    private String selectByIdSql;

    // ... other injected SQL fields

    public ChatRepositoryImpl(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Chat> findById(String id) {
        List<Chat> results = jdbc.query(selectByIdSql,
            Map.of("id", id), new ChatRowMapper());
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    // RowMapper as private static inner class
    private static class ChatRowMapper implements RowMapper<Chat> {
        @Override
        public Chat mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new Chat(
                rs.getString("id"),
                rs.getString("user_id"),
                rs.getString("name"),
                rs.getString("agent_id"),
                rs.getBoolean("is_default"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant(),
                rs.getTimestamp("last_activity_at").toInstant(),
                rs.getInt("message_count")
            );
        }
    }
}
```

### SQL Files (`src/main/resources/sql/chat/`)

```sql
-- insert.sql
INSERT INTO ai_chat.chat (id, user_id, name, agent_id, is_default, created_at, updated_at, last_activity_at, message_count)
VALUES (:id, :userId, :name, :agentId, :isDefault, :createdAt, :updatedAt, :lastActivityAt, :messageCount)

-- selectById.sql
SELECT * FROM ai_chat.chat WHERE id = :id

-- listByUser.sql
SELECT * FROM ai_chat.chat WHERE user_id = :userId ORDER BY last_activity_at DESC

-- findDefaultByUser.sql
SELECT * FROM ai_chat.chat WHERE user_id = :userId AND is_default = TRUE

-- deleteById.sql
DELETE FROM ai_chat.chat WHERE id = :id

-- updateName.sql
UPDATE ai_chat.chat SET name = :name, updated_at = now() WHERE id = :id

-- updateActivity.sql
UPDATE ai_chat.chat SET last_activity_at = :lastActivityAt, message_count = :messageCount, updated_at = now() WHERE id = :id

-- insertMessage.sql
INSERT INTO ai_chat.chat_message (id, chat_id, role, content, sequence_number, tokens_used, created_at, metadata)
VALUES (:id, :chatId, :role, :content, :sequenceNumber, :tokensUsed, :createdAt, :metadata::jsonb)

-- selectMessages.sql
SELECT * FROM ai_chat.chat_message
WHERE chat_id = :chatId AND deleted_at IS NULL
ORDER BY sequence_number ASC
LIMIT :limit OFFSET :offset

-- getNextSequenceNumber.sql
SELECT COALESCE(MAX(sequence_number), 0) + 1 FROM ai_chat.chat_message WHERE chat_id = :chatId

-- softDeleteMessages.sql
UPDATE ai_chat.chat_message SET deleted_at = now() WHERE chat_id = :chatId AND deleted_at IS NULL

-- countMessages.sql
SELECT COUNT(*) FROM ai_chat.chat_message WHERE chat_id = :chatId AND deleted_at IS NULL
```

---

## Service Layer

### ChatService

```java
// chat/service/ChatService.java
public interface ChatService {
    Chat getOrCreateDefaultChat(String userId);
    Chat createChat(String userId, String name, String agentId);
    List<Chat> listChats(String userId);
    Chat requireOwnedChat(String userId, String chatId);
    void deleteChat(String userId, String chatId);
    void renameChat(String userId, String chatId, String name);
    List<ChatMessage> getHistory(String userId, String chatId, int limit, int offset);
    ChatMessage appendUserMessage(String chatId, String content);
    ChatMessage appendAssistantMessage(String chatId, String content, int tokensUsed, Map<String, Object> metadata);
    void updateAgentId(String chatId, String agentId);
}
```

```java
// chat/service/impl/ChatServiceImpl.java
@Service
@Transactional
public class ChatServiceImpl implements ChatService {
    private final ChatRepository chatRepository;
    private final ChatMessageRepository messageRepository;

    @Override
    public Chat getOrCreateDefaultChat(String userId) {
        return chatRepository.findDefaultByUserId(userId)
            .orElseGet(() -> createChat(userId, "New Chat", "auto"));
    }

    @Override
    public Chat createChat(String userId, String name, String agentId) {
        Chat chat = new Chat(
            IdGenerator.generateId(), userId,
            name != null ? name : "New Chat",
            agentId != null ? agentId : "auto",
            !chatRepository.existsByUserId(userId), // first chat is default
            Instant.now(), Instant.now(), Instant.now(), 0
        );
        return chatRepository.insert(chat);
    }

    @Override
    public void deleteChat(String userId, String chatId) {
        Chat chat = requireOwnedChat(userId, chatId);
        messageRepository.softDeleteByChatId(chatId);
        chatRepository.deleteById(chatId);
        // Recreate default if no chats left
        if (chatRepository.findByUserId(userId).isEmpty()) {
            createChat(userId, "New Chat", "auto");
        }
    }

    @Override
    public ChatMessage appendUserMessage(String chatId, String content) {
        int seq = messageRepository.getNextSequenceNumber(chatId);
        ChatMessage msg = new ChatMessage(
            IdGenerator.generateId(), chatId, ChatMessage.ROLE_USER,
            content, seq, null, Instant.now(), Map.of()
        );
        ChatMessage saved = messageRepository.insert(msg);
        chatRepository.updateActivity(chatId, Instant.now(), seq);
        return saved;
    }

    @Override
    public ChatMessage appendAssistantMessage(String chatId, String content, int tokensUsed, Map<String, Object> metadata) {
        int seq = messageRepository.getNextSequenceNumber(chatId);
        ChatMessage msg = new ChatMessage(
            IdGenerator.generateId(), chatId, ChatMessage.ROLE_ASSISTANT,
            content, seq, tokensUsed, Instant.now(), metadata
        );
        ChatMessage saved = messageRepository.insert(msg);
        chatRepository.updateActivity(chatId, Instant.now(), seq);
        return saved;
    }
}
```

### ChatAssistantService

```java
// llm/service/ChatAssistantService.java
public interface ChatAssistantService {
    ChatResponse processMessage(String userId, String chatId, String userMessage);
    SseEmitter streamMessage(String userId, String chatId, String userMessage);
}
```

```java
// llm/service/impl/ChatAssistantServiceImpl.java
@Service
public class ChatAssistantServiceImpl implements ChatAssistantService {
    private final ChatService chatService;
    private final ChatClient chatClient;           // primary chat model
    private final ChatClient toolCallingClient;     // tool-calling model
    private final SessionService sessionService;    // Spring AI Session JDBC
    private final McpServerRegistry mcpServerRegistry;
    private final ChatWorkflowEngine harnessEngine;
    private final ChatStreamActivityPublisher activityPublisher;
    private final List<Advisor> advisorChain;

    @Override
    public SseEmitter streamMessage(String userId, String chatId, String userMessage) {
        SseEmitter emitter = new SseEmitter(120_000L);
        String sessionId = userId + "-" + chatId;

        activityPublisher.register(sessionId, emitter);
        emitter.onCompletion(() -> activityPublisher.unregister(sessionId));
        emitter.onTimeout(() -> activityPublisher.unregister(sessionId));

        chatService.appendUserMessage(chatId, userMessage);
        sessionService.appendUserMessage(sessionId, userMessage);

        sendSseEvent(emitter, "agent", Map.of("type", "agent_start", "agentId", "chat-orchestrator"));

        harnessEngine.execute(sessionId, userMessage, emitter);  // pipeline_stage events

        Flux<ChatResponse> flux = chatClient.prompt()
            .user(userMessage)
            .advisors(a -> a.param(SessionMemoryAdvisor.SESSION_ID_CONTEXT_KEY, sessionId))
            .stream()
            .chatResponse();

        StringBuilder fullResponse = new StringBuilder();

        flux.subscribe(
            response -> {
                String token = response.getResult().getOutput().getText();
                fullResponse.append(token);
                sendSseEvent(emitter, "token", Map.of("t", token));
            },
            error -> emitter.completeWithError(error),
            () -> {
                String content = fullResponse.toString();
                ChatMessage assistant = chatService.appendAssistantMessage(
                    chatId, content, estimateTokens(content), Map.of());
                sessionService.appendAssistantMessage(sessionId, content);

                sendSseEvent(emitter, "agent", Map.of("type", "agent_done", "agentId", "chat-orchestrator"));
                sendSseEvent(emitter, "done", Map.of("id", assistant.id(), "content", content));
                emitter.complete();
            }
        );

        return emitter;
    }

    private void sendSseEvent(SseEmitter emitter, String event, Object data) {
        try {
            emitter.send(SseEmitter.event()
                .name(event)
                .data(data, MediaType.APPLICATION_JSON));
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
    }
}
```

---

## LLM Configuration (`core/config`)

### SpringAIConfig

```java
// core/config/SpringAIConfig.java
@Configuration
@Profile("!test")
@EnableConfigurationProperties(AiChatProperties.class)
public class SpringAIConfig {

    @Bean @Lazy
    ChatClient primaryChatClient(OpenAiChatModelFactory factory, AiChatProperties props) {
        OpenAiChatModel model = factory.create(props.getChat());
        return ChatClient.builder(model).build();
    }

    @Bean @Lazy
    ChatClient toolCallingChatClient(OpenAiChatModelFactory factory, AiChatProperties props) {
        OpenAiChatModel model = factory.create(props.getToolCalling());
        return ChatClient.builder(model).build();
    }

    @Bean @Lazy
    ChatClient alternativeChatClient(OpenAiChatModelFactory factory, AiChatProperties props) {
        OpenAiChatModel model = factory.create(props.getChatAlt());
        return ChatClient.builder(model).build();
    }

    @Bean
    AiConfigStartupValidator startupValidator(AiChatProperties props) {
        return new AiConfigStartupValidator(props);
    }
}
```

### OpenAiChatModelFactory

```java
// core/config/OpenAiChatModelFactory.java
@Component
public class OpenAiChatModelFactory {

    public OpenAiChatModel create(AiChatProperties.ModelConfig config) {
        OpenAiApi api = OpenAiApi.builder()
            .baseUrl(normalizeBaseUrl(config.baseUrl()))
            .apiKey(config.apiKey())
            .build();
        return OpenAiChatModel.builder()
            .openAiApi(api)
            .defaultOptions(OpenAiChatOptions.builder()
                .model(config.model())
                .temperature(config.temperature())
                .maxTokens(config.maxTokens())
                .build())
            .build();
    }

    private String normalizeBaseUrl(String url) {
        if (url != null && !url.endsWith("/v1")) {
            return url.endsWith("/") ? url + "v1" : url + "/v1";
        }
        return url;
    }
}
```

### AiChatProperties

```java
// core/config/AiChatProperties.java
@ConfigurationProperties(prefix = "spring.ai.custom")
@Validated
public record AiChatProperties(
    @Valid ModelConfig chat,
    @Valid ModelConfig chatAlt,
    @Valid ModelConfig toolCalling
) {
    public record ModelConfig(
        @NotBlank String provider,
        @NotBlank String baseUrl,
        String apiKey,
        @NotBlank String model,
        Double temperature,
        Integer maxTokens
    ) {}
}
```

### Advisor Chain

```java
// llm/advisor/AdvisorChainConfig.java
@Configuration
public class AdvisorChainConfig {

    @Bean
    List<Advisor> chatAdvisorChain(
            DateTimeContextAdvisor dateTimeAdvisor,
            MCPToolAdvisor mcpToolAdvisor,
            ToolCallingAdvisor toolCallingAdvisor,
            SessionMemoryAdvisor sessionMemoryAdvisor,
            SimpleLoggerAdvisor loggerAdvisor) {
        return List.of(
            dateTimeAdvisor,
            mcpToolAdvisor,
            toolCallingAdvisor,
            sessionMemoryAdvisor,
            loggerAdvisor
        );
    }

    @Bean
    DateTimeContextAdvisor dateTimeAdvisor() {
        // Class: core/advisor/DateTimeContextAdvisor.java (ported from med-expert-match-ce)
        return DateTimeContextAdvisor.builder()
            .dateTimeFormat("yyyy-MM-dd HH:mm:ss z")
            .build();
    }

    @Bean
    SessionMemoryAdvisor sessionMemoryAdvisor(SessionService sessionService) {
        return SessionMemoryAdvisor.builder()
            .sessionService(sessionService)
            .build();
    }

    @Bean
    ToolCallingAdvisor toolCallingAdvisor(ChatClient toolCallingClient) {
        return ToolCallingAdvisor.builder()
            .chatClient(toolCallingClient)
            .build();
    }
}
```

---

## ChatStreamActivityPublisher (`llm/service`)

Ported from med-expert-match-ce `ChatStreamActivityPublisherImpl`. Bridges Spring application events to SSE `activity` events for the active chat turn.

```java
// llm/service/ChatStreamActivityPublisher.java
public interface ChatStreamActivityPublisher {
    void register(String sessionId, SseEmitter emitter);
    void unregister(String sessionId);
    void publishReasoning(String sessionId, String message);
    void publishTurnSummary(String sessionId, LlmUsageSessionRollup rollup);
}

// llm/service/impl/ChatStreamActivityPublisherImpl.java
@Service
public class ChatStreamActivityPublisherImpl implements ChatStreamActivityPublisher {
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    @EventListener void onToolCallLogged(ToolCallLoggedEvent e) { ... }
    @EventListener void onLlmCallCompleted(LlmCallCompletedEvent e) { ... }
    @EventListener void onTodoUpdate(AgentTodoUpdateEvent e) { ... }

    private void publish(String sessionId, String type, Map<String, Object> fields) {
        // emitter.send(SseEmitter.event().name("activity").data(payload with type field))
    }
}
```

---

## MCP Server Registry (`mcp/registry`)

```java
// mcp/registry/McpServerRegistry.java
public class McpServerRegistry {
    private final Map<String, McpServerInfo> servers = new ConcurrentHashMap<>();

    public void register(String connectionName, McpServerInfo info) { ... }
    public void markDown(String connectionName, String reason) { ... }

    public List<McpServerInfo> getReachableServers() {
        return servers.values().stream()
            .filter(s -> s.status() == ServerStatus.UP)
            .toList();
    }

    public List<ToolCallback> getAllToolCallbacks() {
        List<ToolCallback> callbacks = new ArrayList<>();
        getReachableServers().forEach(info ->
            info.tools().forEach(tool ->
                callbacks.add(new McpToolCallbackWrapper(
                    info.connectionName(), info.client(), tool))));
        return callbacks;
    }

    public String getToolCatalogText() { /* markdown catalog for MCPToolAdvisor */ }
}

public record McpServerInfo(
    String connectionName,
    McpSyncClient client,
    String serverName,
    String version,
    String url,
    ServerStatus status,
    List<ToolDefinition> tools,
    List<ResourceDefinition> resources,
    List<PromptDefinition> prompts
) {}
```

---

## MCP Client Integration (`mcp/`)

### McpClientConfig

```java
// mcp/config/McpClientConfig.java
@Configuration
@Profile("!test")
@EnableConfigurationProperties(McpClientProperties.class)
public class McpClientConfig {

    @Bean
    Map<String, McpSyncClient> mcpClients(McpClientProperties props) {
        Map<String, McpSyncClient> clients = new LinkedHashMap<>();
        props.getConnections().forEach((name, conn) -> {
            if (conn.url() != null && !conn.url().isBlank()) {
                McpSyncClient client = McpSyncClient.using(HttpClientSseClientTransport
                    .builder(conn.url()).build());
                client.initialize();
                clients.put(name, client);
                log.info("MCP client '{}' initialized: {} tools, {} resources, {} prompts",
                    name,
                    client.listTools().size(),
                    client.listResources().size(),
                    client.listPrompts().size());
            }
        });
        return clients;
    }

    @Bean
    McpServerRegistry mcpServerRegistry() {
        return new McpServerRegistry();
    }
}
```

`McpClientConfig` populates the registry on startup. **Per-connection failures are caught** — a unreachable server is marked DOWN; the application context still starts.

```java
// mcp/config/McpClientConfig.java — startup must not fail when MCP is unavailable
props.getConnections().forEach((name, conn) -> {
    try {
        McpSyncClient client = McpSyncClient.using(/* ... */);
        client.initialize();
        registry.register(name, buildInfo(client, conn));
    } catch (Exception e) {
        log.warn("MCP connection '{}' unavailable: {}", name, e.getMessage());
        registry.markDown(name, e.getMessage());
    }
});
```

### McpClientProperties

```java
// mcp/config/McpClientProperties.java
@ConfigurationProperties(prefix = "spring.ai.mcp.client.sse")
public record McpClientProperties(
    Map<String, ConnectionConfig> connections
) {
    public record ConnectionConfig(
        String url,
        boolean tools,
        boolean resources,
        boolean prompts
    ) {}
}
```

### McpToolCallbackWrapper

```java
// mcp/tool/McpToolCallbackWrapper.java
public class McpToolCallbackWrapper implements ToolCallback {

    private final String serverName;
    private final McpSyncClient client;
    private final ToolDefinition toolDef;

    @Override
    public String getName() {
        return toolDef.name();
    }

    @Override
    public String getDescription() {
        return "[MCP:" + serverName + "] " + toolDef.description();
    }

    @Override
    public String getInputTypeSchema() {
        return toolDef.inputSchema();
    }

    @Override
    public String call(String inputJson) {
        return client.callTool(toolDef.name(), inputJson);
    }
}
```

### MCPToolAdvisor

```java
// llm/advisor/MCPToolAdvisor.java
public class MCPToolAdvisor implements Advisor {

    private final McpServerRegistry registry;

    public MCPToolAdvisor(McpServerRegistry registry) {
        this.registry = registry;
    }

    @Override
    public AdvisedRequest advise(AdvisedRequest request, Map<String, Object> context) {
        if (registry.getReachableServers().isEmpty()) {
            return request;
        }
        String toolDefs = registry.getToolCatalogText();
        String enhancedSystemText = request.systemText() + "\n\n" +
            "Available MCP tools:\n" + toolDefs;

        return AdvisedRequest.from(request)
            .systemText(enhancedSystemText)
            .toolCallbacks(registry.getAllToolCallbacks())
            .build();
    }

    @Override
    public int getOrder() {
        return 10; // After DateTimeContextAdvisor (HIGHEST_PRECEDENCE), before ToolCallingAdvisor
    }
}
```

---

## Harness persistence (`V2__harness_schema.sql`)

Harness run state is persisted in a second Flyway migration (M5):

```sql
CREATE TABLE ai_chat.harness_workflow_run (
    run_id       CHAR(24)    PRIMARY KEY,
    session_id   VARCHAR(512) NOT NULL,
    state        VARCHAR(50)  NOT NULL,
    plan_json    JSONB,
    created_at   TIMESTAMPTZ  DEFAULT now(),
    updated_at   TIMESTAMPTZ  DEFAULT now()
);

CREATE TABLE ai_chat.harness_chain_trace (
    id           CHAR(24)    PRIMARY KEY,
    run_id       CHAR(24)    NOT NULL REFERENCES ai_chat.harness_workflow_run(run_id) ON DELETE CASCADE,
    event_type   VARCHAR(50) NOT NULL,
    payload      JSONB,
    created_at   TIMESTAMPTZ  DEFAULT now()
);
```

---

## Harness Workflow Engine (`llm/harness`)

### ChatWorkflowEngine

```java
// llm/harness/ChatWorkflowEngine.java
@Component
public class ChatWorkflowEngine {
    private final AgentPlannerService planner;
    private final AgentResponseVerifier verifier;
    private final PolicyGateService policyGate;
    private final HarnessWorkflowRunStore runStore;
    private final HarnessChainTraceStore traceStore;
    private final HarnessProperties properties;

    /**
     * Executes a chat workflow with planning, tool execution, verification, and policy gates.
     * Emits SSE events for each stage.
     */
    public HarnessResult execute(String sessionId, String userMessage,
                                  ChatClient chatClient, SseEmitter emitter) {
        String runId = IdGenerator.generateId();

        // Stage 1: TASK_CREATED
        traceStore.record(runId, "TASK_CREATED", Map.of("message", userMessage));
        emitStage(emitter, "TASK_CREATED");

        // Stage 2: PLANNING
        emitStage(emitter, "PLANNING");
        AgentPlan plan = planner.buildPlan(sessionId, userMessage);
        runStore.save(runId, sessionId, "PLANNING", plan);
        emitTodoUpdate(emitter, plan);

        // Stage 3: CONTEXT_BUILT
        emitStage(emitter, "CONTEXT_BUILT");
        // Context is built by MCPToolAdvisor + SessionMemoryAdvisor

        // Stage 4: TOOLS_EXECUTED
        emitStage(emitter, "TOOLS_EXECUTED");
        // Tool execution happens via ToolCallingAdvisor during LLM call
        // Harness tracks tool calls from response metadata

        // Stage 5: VERIFYING
        emitStage(emitter, "VERIFYING");
        VerificationResult verification = verifier.verify(plan, /* tool outputs */);
        if (!verification.passed() && properties.retryOnVerifyFail()) {
            // Retry logic (max iterations)
        }

        // Stage 6: POLICY_GATE
        if (properties.policyGateEnabled()) {
            emitStage(emitter, "POLICY_GATE");
            PolicyDecision decision = policyGate.review(/* response */);
            if (decision == PolicyDecision.REJECT) {
                runStore.updateState(runId, "FAILED");
                return HarnessResult.failed("Policy gate rejected response");
            }
        }

        // Stage 7: DONE
        runStore.updateState(runId, "DONE");
        emitStage(emitter, "DONE");
        return HarnessResult.success();
    }

    private void emitStage(SseEmitter emitter, String stage) {
        sendEvent(emitter, "pipeline_stage", Map.of("stage", stage));
    }

    private void emitTodoUpdate(SseEmitter emitter, AgentPlan plan) {
        sendEvent(emitter, "activity", Map.of(
            "type", "todo_update",
            "steps", plan.steps()
        ));
    }
}
```

### AgentPlannerService

```java
// llm/harness/AgentPlannerService.java
public interface AgentPlannerService {
    AgentPlan buildPlan(String sessionId, String userMessage);
}

// llm/harness/AgentPlannerServiceImpl.java
@Service
public class AgentPlannerServiceImpl implements AgentPlannerService {
    private final ChatClient toolCallingClient;

    @Override
    public AgentPlan buildPlan(String sessionId, String userMessage) {
        String prompt = """
            Create a step-by-step plan to answer the user's question.
            Consider available MCP tools for data retrieval.
            Output as JSON with steps and acceptance criteria.
            User message: %s
            """.formatted(userMessage);

        String planJson = toolCallingClient.prompt()
            .user(prompt)
            .call()
            .content();

        return parsePlan(planJson);
    }
}

public record AgentPlan(
    String planId,
    List<PlanStep> steps,
    List<String> acceptanceCriteria
) {}

public record PlanStep(
    int order,
    String description,
    String toolName,
    String status  // PENDING, IN_PROGRESS, COMPLETED, FAILED
) {}
```

### AgentResponseVerifier

```java
// llm/harness/AgentResponseVerifier.java
@Component
public class AgentResponseVerifier {

    public VerificationResult verify(AgentPlan plan, Map<String, Object> toolOutputs) {
        List<String> failures = new ArrayList<>();

        for (PlanStep step : plan.steps()) {
            if (step.toolName() != null && !toolOutputs.containsKey(step.toolName())) {
                failures.add("Missing output for tool: " + step.toolName());
            }
        }

        for (String criterion : plan.acceptanceCriteria()) {
            // Check if response satisfies each criterion
            // (simplified: structural checks only)
        }

        return new VerificationResult(failures.isEmpty(), failures);
    }
}

public record VerificationResult(boolean passed, List<String> failures) {}
```

### PolicyGateService

```java
// llm/harness/PolicyGateService.java
@Component
public class PolicyGateService {

    public PolicyDecision review(String responseContent) {
        // Basic safety checks:
        // - No obviously harmful content
        // - Response is not empty
        // - Response is not a refusal without explanation
        if (responseContent == null || responseContent.isBlank()) {
            return PolicyDecision.REJECT;
        }
        return PolicyDecision.APPROVE;
    }
}

public enum PolicyDecision { APPROVE, REJECT, NEEDS_REVIEW }
```

### HarnessWorkflowRunStore

```java
// llm/harness/HarnessWorkflowRunStore.java
public interface HarnessWorkflowRunStore {
    void save(String runId, String sessionId, String state, AgentPlan plan);
    void updateState(String runId, String state);
    Optional<HarnessWorkflowRun> findByRunId(String runId);
}

public record HarnessWorkflowRun(
    String runId,
    String sessionId,
    String state,
    String planJson,
    Instant createdAt,
    Instant updatedAt
) {}
```

### HarnessProperties

```java
// llm/config/HarnessProperties.java
@ConfigurationProperties(prefix = "ai-chat.harness")
public record HarnessProperties(
    int maxIterations,
    boolean retryOnVerifyFail,
    boolean policyGateEnabled,
    boolean humanCheckpointEnabled
) {}
```

---

## REST API (`chat/rest`)

### ChatController

```java
// chat/rest/ChatController.java
@RestController
@RequestMapping("/api/v1/chats")
public class ChatController {
    private final ChatService chatService;
    private final ChatAssistantService assistantService;
    private final UserContext userContext;

    @GetMapping
    List<Chat> listChats() {
        return chatService.listChats(userContext.getUserId());
    }

    @PostMapping
    Chat createChat(@RequestBody CreateChatRequest request) {
        return chatService.createChat(
            userContext.getUserId(),
            request.name(),
            request.agentId()
        );
    }

    @GetMapping("/{chatId}/history")
    List<ChatMessage> history(
            @PathVariable String chatId,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        return chatService.getHistory(userContext.getUserId(), chatId, limit, offset);
    }

    @DeleteMapping("/{chatId}")
    void deleteChat(@PathVariable String chatId) {
        chatService.deleteChat(userContext.getUserId(), chatId);
    }

    @PutMapping("/{chatId}/name")
    void renameChat(@PathVariable String chatId, @RequestBody RenameRequest request) {
        chatService.renameChat(userContext.getUserId(), chatId, request.name());
    }

    @PostMapping("/{chatId}/messages/stream")
    SseEmitter streamMessage(
            @PathVariable String chatId,
            @RequestBody SendMessageRequest request) {
        return assistantService.streamMessage(
            userContext.getUserId(), chatId, request.content());
    }
}

public record CreateChatRequest(String name, String agentId) {}
public record RenameRequest(String name) {}
public record SendMessageRequest(String content) {}
```

---

## Web Layer (`web/controller`)

### ChatWebController

```java
// web/controller/ChatWebController.java
@Controller
public class ChatWebController {
    private final ChatService chatService;

    @GetMapping("/")
    String chatPage(Model model, HttpServletRequest request) {
        String userId = resolveUserId(request);
        Chat defaultChat = chatService.getOrCreateDefaultChat(userId);
        return "redirect:/chat/" + defaultChat.id();
    }

    @GetMapping("/chat/{chatId}")
    String chatView(@PathVariable String chatId, Model model, HttpServletRequest request) {
        String userId = resolveUserId(request);
        Chat chat = chatService.requireOwnedChat(userId, chatId);
        List<Chat> allChats = chatService.listChats(userId);

        model.addAttribute("chat", chat);
        model.addAttribute("chats", allChats);
        model.addAttribute("userId", userId);
        return "chat";
    }
}
```

---

## Frontend (`src/main/resources`)

### chat.html (Thymeleaf template)

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>AI Chat</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="/css/chat.css" rel="stylesheet">
</head>
<body>
<div class="container-fluid vh-100 d-flex flex-column p-0">
    <!-- Header -->
    <nav class="navbar navbar-light bg-light border-bottom px-3">
        <span class="navbar-brand">AI Chat</span>
        <button id="newChatBtn" class="btn btn-outline-primary btn-sm">+ New Chat</button>
    </nav>

    <div class="row flex-grow-1 g-0 overflow-hidden">
        <!-- Sidebar -->
        <div class="col-md-3 col-lg-2 border-end bg-light d-flex flex-column" id="sidebar">
            <div class="chat-list flex-grow-1 overflow-auto p-2" id="chatList">
                <!-- Rendered by JS from /api/v1/chats -->
            </div>
            <div class="p-2 border-top">
                <button id="deleteAllBtn" class="btn btn-outline-danger btn-sm w-100">
                    Delete All Chats
                </button>
            </div>
        </div>

        <!-- Main chat area -->
        <div class="col d-flex flex-column">
            <!-- Messages -->
            <div class="flex-grow-1 overflow-auto p-3" id="messagePanel">
                <!-- Rendered by JS from /api/v1/chats/{chatId}/history -->
            </div>

            <!-- Agent Progress Panel (collapsible) -->
            <div id="agentPanel" class="border-top bg-light p-2 d-none">
                <div class="d-flex justify-content-between align-items-center">
                    <strong id="agentPanelTitle">Agent Progress</strong>
                    <button class="btn btn-sm btn-outline-secondary" id="toggleAgentPanel">−</button>
                </div>
                <div id="agentActivityList" class="small mt-1"></div>
            </div>

            <!-- Composer -->
            <div class="border-top p-3 bg-white">
                <div class="input-group">
                    <textarea id="composer" class="form-control" rows="2"
                        placeholder="Type your message... (Enter to send, Shift+Enter for newline)"></textarea>
                    <button id="sendBtn" class="btn btn-primary">Send</button>
                </div>
            </div>
        </div>
    </div>
</div>

<script th:inline="javascript">
    window.AICHAT_CONFIG = {
        userId: /*[[${userId}]]*/ '',
        chatId: /*[[${chat.id}]]*/ '',
        agentId: /*[[${chat.agentId}]]*/ 'auto'
    };
</script>
<script src="https://cdn.jsdelivr.net/npm/marked/marked.min.js"></script>
<script src="https://cdn.jsdelivr.net/npm/dompurify@3/dist/purify.min.js"></script>
<script src="/js/main.js"></script>
<script src="/js/chat.js"></script>
</body>
</html>
```

### chat.js (SSE streaming client)

```javascript
// static/js/chat.js — SSE streaming chat client
(function() {
    const config = window.AICHAT_CONFIG;
    let currentAssistantMessage = null;
    let agentPanelActive = false;

    // --- Chat list ---
    async function loadChatList() {
        const resp = await fetch('/api/v1/chats', {
            headers: { 'X-User-Id': config.userId }
        });
        const chats = await resp.json();
        renderChatList(chats);
    }

    function renderChatList(chats) {
        const list = document.getElementById('chatList');
        list.innerHTML = chats.map(c => `
            <div class="chat-item ${c.id === config.chatId ? 'active' : ''}"
                 data-chat-id="${c.id}">
                <a href="/chat/${c.id}" class="text-decoration-none">
                    <div class="chat-name">${escapeHtml(c.name || 'New Chat')}</div>
                    <div class="chat-meta">${c.messageCount} messages</div>
                </a>
                <button class="btn btn-sm text-danger delete-chat-btn"
                        data-chat-id="${c.id}">×</button>
            </div>
        `).join('');
    }

    // --- Message history ---
    async function loadHistory() {
        const resp = await fetch(`/api/v1/chats/${config.chatId}/history?limit=100`);
        const messages = await resp.json();
        renderMessages(messages);
    }

    function renderMessages(messages) {
        const panel = document.getElementById('messagePanel');
        panel.innerHTML = messages.map(m => renderMessage(m)).join('');
        panel.scrollTop = panel.scrollHeight;
    }

    function renderMessage(msg) {
        const roleClass = msg.role === 'user' ? 'message-user' : 'message-assistant';
        const roleBadge = msg.role === 'user' ? 'You' : 'AI';
        const html = marked.parse(msg.content);
        const sanitized = DOMPurify.sanitize(html);
        return `
            <div class="message ${roleClass}" data-message-id="${msg.id}">
                <div class="message-role">${roleBadge}</div>
                <div class="message-content">${sanitized}</div>
            </div>
        `;
    }

    // --- Send message (SSE streaming) ---
    async function sendMessage() {
        const composer = document.getElementById('composer');
        const content = composer.value.trim();
        if (!content) return;

        composer.value = '';
        composer.disabled = true;
        document.getElementById('sendBtn').disabled = true;

        // Add user message to panel
        appendUserMessage(content);

        // Create assistant message placeholder
        currentAssistantMessage = createAssistantMessagePlaceholder();

        // Open agent panel
        openAgentPanel();

        // Stream response
        const response = await fetch(`/api/v1/chats/${config.chatId}/messages/stream`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-User-Id': config.userId
            },
            body: JSON.stringify({ content })
        });

        const reader = response.body.getReader();
        const decoder = new TextDecoder();
        let buffer = '';

        while (true) {
            const { done, value } = await reader.read();
            if (done) break;

            buffer += decoder.decode(value, { stream: true });
            const events = buffer.split('\n\n');
            buffer = events.pop(); // keep incomplete event in buffer

            for (const eventBlock of events) {
                processSseEvent(eventBlock);
            }
        }

        // Finalize
        finalizeStream();
    }

    function processSseEvent(eventBlock) {
        const lines = eventBlock.split('\n');
        let eventType = 'message';
        let data = '';

        for (const line of lines) {
            if (line.startsWith('event:')) eventType = line.substring(6).trim();
            else if (line.startsWith('data:')) data += line.substring(5).trim();
        }

        if (!data) return;

        try {
            const payload = JSON.parse(data);
            switch (eventType) {
                case 'token':
                    appendToken(payload.t ?? payload);
                    break;
                case 'activity':
                    addAgentActivity(payload);
                    break;
                case 'agent':
                    handleAgentEvent(payload);
                    break;
                case 'pipeline_stage':
                    updatePipelineStage(payload);
                    break;
                case 'done':
                    // handled by finalizeStream
                    break;
            }
        } catch (e) {
            // Non-JSON data (plain text token)
            if (eventType === 'token' || eventType === 'message') {
                appendToken(data);
            }
        }
    }

    function appendToken(text) {
        if (!currentAssistantMessage || text == null) return;
        const contentEl = currentAssistantMessage.querySelector('.message-content');
        contentEl.textContent += text;
        // Re-render markdown
        const html = marked.parse(contentEl.textContent);
        contentEl.innerHTML = DOMPurify.sanitize(html);
        document.getElementById('messagePanel').scrollTop =
            document.getElementById('messagePanel').scrollHeight;
    }

    function addAgentActivity(activity) {
        const list = document.getElementById('agentActivityList');
        const entry = document.createElement('div');
        entry.className = 'activity-entry';

        let icon = '';
        switch (activity.type) {
            case 'tool_call':
                icon = '🔧';
                entry.innerHTML = `${icon} Tool: <strong>${activity.toolName || activity.name}</strong>`;
                if (activity.arguments) {
                    const argsPre = document.createElement('pre');
                    argsPre.className = 'activity-detail';
                    argsPre.textContent = JSON.stringify(activity.arguments, null, 2);
                    entry.appendChild(argsPre);
                }
                break;
            case 'reasoning':
                icon = '💭';
                const details = document.createElement('details');
                details.innerHTML = `<summary>${icon} Reasoning</summary><p>${activity.text}</p>`;
                entry.appendChild(details);
                break;
            case 'todo_update':
                icon = '📋';
                entry.innerHTML = `${icon} Plan: ${activity.steps.map(s =>
                    `${s.description} [${s.status}]`).join(' → ')}`;
                break;
            case 'llm_call':
                icon = '🤖';
                entry.innerHTML = `${icon} LLM call (${activity.tokens || '?'} tokens)`;
                break;
        }

        list.appendChild(entry);
        showAgentPanel();
    }

    function handleAgentEvent(event) {
        if (event.type === 'agent_start') {
            document.getElementById('agentPanelTitle').textContent =
                `Agent: ${event.name}`;
        } else if (event.type === 'agent_done') {
            collapseAgentPanel(event.summary);
        }
    }

    function finalizeStream() {
        document.getElementById('composer').disabled = false;
        document.getElementById('sendBtn').disabled = false;
        document.getElementById('composer').focus();

        if (currentAssistantMessage) {
            currentAssistantMessage.classList.add('message-complete');
        }

        collapseAgentPanel(null);
        loadChatList(); // refresh sidebar counts
    }

    function collapseAgentPanel(summary) {
        const panel = document.getElementById('agentPanel');
        const activityList = document.getElementById('agentActivityList');
        const count = activityList.children.length;

        if (summary) {
            document.getElementById('agentPanelTitle').textContent = summary;
        } else if (count > 0) {
            document.getElementById('agentPanelTitle').textContent =
                `▸ ${count} step(s) completed — click to expand`;
        }

        activityList.classList.add('d-none');
        document.getElementById('toggleAgentPanel').textContent = '+';
    }

    function openAgentPanel() {
        document.getElementById('agentActivityList').innerHTML = '';
        showAgentPanel();
    }

    function showAgentPanel() {
        const panel = document.getElementById('agentPanel');
        panel.classList.remove('d-none');
        document.getElementById('agentActivityList').classList.remove('d-none');
        document.getElementById('toggleAgentPanel').textContent = '−';
    }

    // --- Init ---
    document.getElementById('sendBtn').addEventListener('click', sendMessage);
    document.getElementById('composer').addEventListener('keydown', (e) => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            sendMessage();
        }
    });
    document.getElementById('newChatBtn').addEventListener('click', async () => {
        const resp = await fetch('/api/v1/chats', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-User-Id': config.userId
            },
            body: JSON.stringify({ name: 'New Chat', agentId: 'auto' })
        });
        const chat = await resp.json();
        window.location.href = '/chat/' + chat.id;
    });
    document.getElementById('toggleAgentPanel').addEventListener('click', () => {
        const list = document.getElementById('agentActivityList');
        const btn = document.getElementById('toggleAgentPanel');
        if (list.classList.contains('d-none')) {
            showAgentPanel();
        } else {
            collapseAgentPanel(null);
        }
    });

    // Load initial data
    loadChatList();
    loadHistory();
})();
```

### chat.css

```css
/* static/css/chat.css */
.message { margin-bottom: 1rem; padding: 0.75rem; border-radius: 0.5rem; }
.message-user { background: #e3f2fd; }
.message-assistant { background: #f5f5f5; }
.message-role { font-size: 0.75rem; font-weight: 600; color: #666; margin-bottom: 0.25rem; }
.message-content p:last-child { margin-bottom: 0; }
.message-content pre { background: #263238; color: #eeffff; padding: 0.75rem; border-radius: 0.25rem; overflow-x: auto; }
.message-content code { font-size: 0.875rem; }
.message-content details { margin: 0.5rem 0; }
.message-content details summary { cursor: pointer; color: #666; font-size: 0.875rem; }
.message-content details[open] summary { margin-bottom: 0.5rem; }

.chat-item { padding: 0.5rem; border-bottom: 1px solid #dee2e6; cursor: pointer; display: flex; justify-content: space-between; align-items: center; }
.chat-item.active { background: #e3f2fd; }
.chat-item:hover { background: #f0f0f0; }
.chat-name { font-size: 0.875rem; font-weight: 500; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 180px; }
.chat-meta { font-size: 0.75rem; color: #999; }
.delete-chat-btn { visibility: hidden; }
.chat-item:hover .delete-chat-btn { visibility: visible; }

.activity-entry { padding: 0.25rem 0; border-bottom: 1px solid #eee; font-size: 0.8rem; }
.activity-entry:last-child { border-bottom: none; }
.activity-detail { font-size: 0.75rem; background: #f8f9fa; padding: 0.25rem; margin: 0.25rem 0 0 1rem; border-radius: 0.25rem; max-height: 100px; overflow-y: auto; }

#agentPanel { max-height: 200px; overflow-y: auto; transition: max-height 0.3s; }
#agentPanel.collapsed { max-height: 40px; overflow: hidden; }

.message-complete { }
```

---

## User Identity (`core/security`)

```java
// core/security/UserContext.java
@Component
public class UserContext {
    public String getUserId() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes sa) {
            HttpServletRequest request = sa.getRequest();
            // Header takes priority
            String headerId = request.getHeader("X-User-Id");
            if (headerId != null && !headerId.isBlank()) return headerId;
            // Fallback to cookie
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie c : cookies) {
                    if ("aichat-user-id".equals(c.getName())) return c.getValue();
                }
            }
        }
        return "anonymous";
    }
}
```

```java
// core/config/SecurityConfig.java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/chat/**", "/css/**", "/js/**",
                    "/api/v1/**", "/actuator/health", "/actuator/info").permitAll()
                .anyRequest().authenticated()
            )
            .httpBasic(AbstractHttpConfigurer::disable)
            .build();
    }
}
```

---

## Session Memory (Spring AI Session JDBC)

Session memory uses the community `spring-ai-starter-session-jdbc` library (v0.3.0), same as `med-expert-match-ce`.

```java
// llm/config/AgentSessionProperties.java
@ConfigurationProperties(prefix = "spring.ai.session.jdbc")
public record AgentSessionProperties(
    String schema,
    Compaction compaction
) {
    public record Compaction(
        String strategy,      // "turn-window"
        int maxTurns,         // 20
        int maxTokens,        // 4000
        int windowTurns       // 30
    ) {}
}
```

Session ID format: `{userId}-{chatId}` — same as `med-expert-match-ce`.

---

## Structured Output

The chat model supports **Structured Output** via Spring AI's typed response records. When the LLM needs to produce structured data (e.g., MCP tool call arguments, plan steps), it uses JSON schema-constrained generation.

```java
// Example: structured output for agent planning
public record AgentPlanOutput(
    @JsonProperty("steps") List<PlanStepOutput> steps,
    @JsonProperty("acceptance_criteria") List<String> acceptanceCriteria
) {}

public record PlanStepOutput(
    @JsonProperty("order") int order,
    @JsonProperty("description") String description,
    @JsonProperty("tool") String toolName
) {}

// Usage in AgentPlannerService:
AgentPlanOutput plan = chatClient.prompt()
    .user("Create a plan to answer: " + userMessage)
    .call()
    .entity(AgentPlanOutput.class);
```

---

## Related documentation

- [README.md](README.md) — documentation index and naming
- [../README.md](../README.md) — project overview
- [01-requirements.md](01-requirements.md) — SRS with goals, MCP surface, milestones
- [02-architecture.md](02-architecture.md) — system diagram, Modulith modules, stack
- [04-testing.md](04-testing.md) — test strategy
- [05-deployment.md](05-deployment.md) — config, Docker, env vars
