# Architecture
## Software Architecture Document (SAD)

**Version:** 1.0.0
**Date:** 2026-06-17
**Requirements:** [01-requirements.md](01-requirements.md)
**Reference pattern:** [med-expert-match-ce](https://github.com/berdachuk/med-expert-match-ce)

System design for `ai-chat`: deployment topology, Spring Modulith modules, stack versions, and key design decisions. Implementation details: [03-design.md](03-design.md). Operations: [05-deployment.md](05-deployment.md).

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────┐
│  Browser (Thymeleaf SSR + vanilla JS)                                   │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │  chat.html — sidebar, message panel, agent progress, composer    │   │
│  │  chat.js  — SSE streaming, Markdown rendering, agent panel       │   │
│  └──────────────────────────────┬───────────────────────────────────┘   │
└─────────────────────────────────┼───────────────────────────────────────┘
                                  │  HTTP + SSE (text/event-stream)
                                  ▼
┌─────────────────────────────────────────────────────────────────────────┐
│  ai-chat  :8080  (single module, Spring Modulith)                       │
│                                                                         │
│  web/ — Thymeleaf SSR controllers                                       │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │  ChatWebController — serves chat.html, sidebar fragments         │   │
│  └──────────────────────────────┬───────────────────────────────────┘   │
│                                                                         │
│  chat/ — REST API + domain + persistence                                │
│  ┌──────────────────┬───────────────────┬──────────────────────────┐   │
│  │  ChatController  │  ChatService      │  ChatRepository (JDBC)    │   │
│  │  (REST /api/v1)  │  + impl           │  + impl                   │   │
│  └────────┬─────────┴─────────┬─────────┴──────────────┬───────────┘   │
│           │                   │                        │                │
│  llm/ — orchestration, harness, advisors, session memory                │
│  ┌──────────────────┬───────────────────┬──────────────────────────┐   │
│  │ ChatAssistant    │ ChatWorkflowEngine│ AgentPlannerService       │   │
│  │ Service + impl   │ (harness)         │ + impl                    │   │
│  │                  │                   │                           │   │
│  │ ChatStream       │ AgentResponse     │ SessionMemoryAdvisor      │   │
│  │ ActivityPublisher│ Verifier          │                           │   │
│  └────────┬─────────┴─────────┬─────────┴──────────────┬───────────┘   │
│           │                   │                        │                │
│  mcp/ — MCP client, registry, tool wrappers                             │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │  McpClientConfig → McpSyncClient(s) → McpServerRegistry          │   │
│  │  McpToolCallbackWrapper → MCPToolAdvisor → ToolCallingAdvisor    │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│                                                                         │
│  core/ — config, security, health, util                                 │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │  SpringAIConfig (ChatModel factory)  │  SecurityConfig           │   │
│  │  IdGenerator                         │  InjectSql + BPP          │   │
│  └──────────────────────────────────────────────────────────────────┘   │
└───────────┬─────────────────────┬─────────────────────┬─────────────────┘
            │                     │                     │
            ▼                     ▼                     ▼
┌────────────────────┐  ┌────────────────────┐  ┌──────────────────────────┐
│  PostgreSQL 17     │  │  Ollama            │  │  MCP Server(s)           │
│  ai_chat schema    │  │  gemma4:31b-cloud  │  │  ai-architect-6-mcp      │
│  chat + messages   │  │  gemma4:12b        │  │  (medical-mcp-server)    │
│  ai_session (JDBC) │  │  functiongemma     │  │  :8092/sse               │
└────────────────────┘  └────────────────────┘  └──────────────────────────┘
```

---

## Module Structure (Spring Modulith)

Single Maven module with **package-based application modules** — same layout as [`med-expert-match-ce`](https://github.com/berdachuk/med-expert-match-ce/tree/main/src/main/java/com/berdachuk/medexpertmatch).

```
ai-chat/
├── pom.xml
├── docker-compose.yml
├── Dockerfile
└── src/
    ├── main/
    │   ├── java/com/berdachuk/aichat/
    │   │   ├── AiChatApplication.java
    │   │   ├── core/                         # config, security, health, util
    │   │   │   ├── config/
    │   │   │   │   ├── SpringAIConfig.java           # ChatModel factory
    │   │   │   │   ├── SecurityConfig.java           # Permit-all for local/dev
    │   │   │   │   ├── AiConfigStartupValidator.java # Fail-fast on missing config
    │   │   │   │   └── OpenAiChatModelFactory.java   # Manual ChatModel builder
    │   │   │   ├── advisor/
    │   │   │   │   └── DateTimeContextAdvisor.java
    │   │   │   ├── util/
    │   │   │   │   └── IdGenerator.java              # 24-char hex ID generator
    │   │   │   ├── repository/
    │   │   │   │   └── sql/
    │   │   │   │       ├── InjectSql.java            # @InjectSql annotation
    │   │   │   │       └── SqlInjectBeanPostProcessor.java
    │   │   │   └── package-info.java
    │   │   ├── chat/
    │   │   │   ├── domain/
    │   │   │   │   ├── Chat.java                    # record: id, userId, name, agentId, …
    │   │   │   │   └── ChatMessage.java             # record: id, chatId, role, content, …
    │   │   │   ├── repository/
    │   │   │   │   ├── ChatRepository.java          # interface
    │   │   │   │   ├── ChatMessageRepository.java   # interface
    │   │   │   │   └── impl/
    │   │   │   │       ├── ChatRepositoryImpl.java
    │   │   │   │       └── ChatMessageRepositoryImpl.java
    │   │   │   ├── service/
    │   │   │   │   ├── ChatService.java             # interface
    │   │   │   │   └── impl/
    │   │   │   │       └── ChatServiceImpl.java
    │   │   │   ├── rest/
    │   │   │   │   └── ChatController.java          # REST /api/v1/chats
    │   │   │   └── package-info.java
    │   │   ├── llm/
    │   │   │   ├── service/
    │   │   │   │   ├── ChatAssistantService.java     # interface (or chat/service per reference)
    │   │   │   │   ├── ChatStreamActivityPublisher.java
    │   │   │   │   └── impl/
    │   │   │   │       ├── ChatAssistantServiceImpl.java
    │   │   │   │       └── ChatStreamActivityPublisherImpl.java
    │   │   │   ├── harness/
    │   │   │   │   ├── ChatWorkflowEngine.java       # Harness orchestrator
    │   │   │   │   ├── AgentPlannerService.java      # Plan builder
    │   │   │   │   ├── AgentResponseVerifier.java    # Output verification
    │   │   │   │   ├── PolicyGateService.java        # Safety/quality review
    │   │   │   │   ├── HarnessWorkflowRunStore.java  # State persistence
    │   │   │   │   └── HarnessChainTraceStore.java   # Event tracing
    │   │   │   ├── advisor/
    │   │   │   │   ├── MCPToolAdvisor.java
    │   │   │   │   └── AdvisorChainConfig.java
    │   │   │   ├── config/
    │   │   │   │   ├── HarnessConfiguration.java
    │   │   │   │   └── AgentSessionProperties.java
    │   │   │   └── package-info.java
    │   │   ├── mcp/
    │   │   │   ├── config/
    │   │   │   │   └── McpClientConfig.java          # McpSyncClient beans
    │   │   │   ├── registry/
    │   │   │   │   └── McpServerRegistry.java        # Server catalog + tool callbacks
    │   │   │   ├── tool/
    │   │   │   │   └── McpToolCallbackWrapper.java   # MCP tool → ToolCallback
    │   │   │   └── package-info.java
    │   │   ├── web/
    │   │   │   ├── controller/
    │   │   │   │   └── ChatWebController.java        # Thymeleaf SSR
    │   │   │   └── package-info.java
    │   │   └── system/
    │   │       ├── McpConnectionHealthIndicator.java
    │   │       └── package-info.java
    │   └── resources/
    │       ├── application.yml
    │       ├── db/migration/
    │       │   └── V1__init_chat_schema.sql
    │       ├── sql/chat/
    │       │   ├── insert.sql
    │       │   ├── selectById.sql
    │       │   ├── listByUser.sql
    │       │   ├── deleteById.sql
    │       │   ├── updateName.sql
    │       │   ├── updateActivity.sql
    │       │   ├── insertMessage.sql
    │       │   ├── selectMessages.sql
    │       │   ├── getNextSequenceNumber.sql
    │       │   ├── softDeleteMessages.sql
    │       │   └── countMessages.sql
    │       ├── templates/
    │       │   ├── chat.html
    │       │   └── fragments/
    │       │       ├── layout.html
    │       │       ├── chat-sidebar.html
    │       │       └── agent-panel.html
    │       └── static/
    │           ├── css/
    │           │   └── chat.css
    │           └── js/
    │               ├── chat.js
    │               └── main.js
    └── test/java/com/berdachuk/aichat/
        ├── ModulithArchitectureTest.java
        ├── chat/
        │   ├── repository/ChatRepositoryImplTest.java
        │   └── service/ChatServiceImplTest.java
        ├── llm/
        │   └── service/ChatAssistantServiceImplTest.java
        └── integration/
            ├── AbstractPostgresIntegrationTest.java
            ├── FlywaySchemaIntegrationTest.java
            ├── ChatControllerIntegrationTest.java
            └── McpClientIntegrationTest.java
```

### Modulith dependency rules (`@ApplicationModule`)

```java
// core/package-info.java
@ApplicationModule(allowedDependencies = {})
package com.berdachuk.aichat.core;

// chat/package-info.java
@ApplicationModule(allowedDependencies = "core :: *")
package com.berdachuk.aichat.chat;

// llm/package-info.java
@ApplicationModule(allowedDependencies = {"core :: *", "chat :: *", "mcp :: *"})
package com.berdachuk.aichat.llm;

// mcp/package-info.java
@ApplicationModule(allowedDependencies = {"core :: *"})
package com.berdachuk.aichat.mcp;

// web/package-info.java
@ApplicationModule(allowedDependencies = {"core :: *", "chat :: *", "llm :: *"})
package com.berdachuk.aichat.web;

// system/package-info.java
@ApplicationModule(allowedDependencies = {"core :: *", "mcp :: *"})
package com.berdachuk.aichat.system;
```

### Interface / implementation convention

| Pattern | med-expert-match-ce example | ai-chat |
|---|---|---|
| Repository API | `chat/repository/ChatRepository.java` | Same package layout |
| Repository impl | `chat/repository/impl/ChatRepositoryImpl.java` | JDBC via `NamedParameterJdbcTemplate` |
| Service API | `chat/service/ChatService.java` | `ChatService`, `ChatAssistantService` |
| Service impl | `chat/service/impl/ChatServiceImpl.java` | `*ServiceImpl` in `service/impl/` |
| REST layer | `chat/rest/ChatController.java` | Injects service interfaces only |
| Web layer | `web/controller/ChatWebController.java` | Thymeleaf SSR, injects service interfaces |

**Rules:** no `@Service` / `@Repository` on interfaces; impl classes are the only `@Component` stereotypes; REST/web and cross-module code never references `*.impl.*` types.

### Modulith verification (CI)

```java
@ApplicationModuleTest
class ModulithArchitectureTest {
    @Test
    void verifyModuleBoundaries() {
        ApplicationModules.of(AiChatApplication.class).verify();
    }
}
```

---

## Stack & Versions

Aligned with `med-expert-match-ce` versions:

| Dependency | Version | Notes |
|---|---|---|
| Spring Boot | 4.1.0 | Parent POM |
| Spring AI BOM | 2.0.0 | |
| Spring Modulith BOM | 2.1.0 | |
| Java | 21 | |
| `spring-boot-starter-web` | (Boot BOM) | REST + Thymeleaf |
| `spring-boot-starter-thymeleaf` | (Boot BOM) | SSR templates |
| `spring-boot-starter-jdbc` | (Boot BOM) | NamedParameterJdbcTemplate |
| `spring-boot-starter-flyway` | (Boot BOM) | Migrations |
| `flyway-database-postgresql` | (Boot BOM) | |
| `postgresql` | 42.7.11 | |
| `spring-modulith-core` | 2.1.0 | `@ApplicationModule` |
| `spring-ai-starter-model-openai` | 2.0.0 | ChatModel + ToolCalling |
| `spring-ai-starter-session-jdbc` | 0.3.0 | Session memory (community) |
| `spring-ai-agent-utils` | 0.10.0 | TodoWrite, AskUserQuestion tools |
| `spring-ai-starter-mcp-client` | 2.0.0 | MCP client SSE |
| `caffeine` | 3.2.4 | Local cache |
| `spring-boot-starter-actuator` | (Boot BOM) | |
| `spring-boot-starter-security` | (Boot BOM) | |
| `spring-boot-starter-validation` | (Boot BOM) | |
| `spring-modulith-starter-test` | 2.1.0 | Modulith verification |
| Testcontainers | 2.0.5 | Integration tests |

### Parent `pom.xml` (single module)

```xml
<properties>
  <java.version>21</java.version>
  <spring-ai.version>2.0.0</spring-ai.version>
  <spring-modulith.version>2.1.0</spring-modulith.version>
  <spring-ai-agent.version>0.10.0</spring-ai-agent.version>
  <spring-ai-session.version>0.3.0</spring-ai-session.version>
</properties>

<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>org.springframework.ai</groupId>
      <artifactId>spring-ai-bom</artifactId>
      <version>${spring-ai.version}</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
    <dependency>
      <groupId>org.springframework.modulith</groupId>
      <artifactId>spring-modulith-bom</artifactId>
      <version>${spring-modulith.version}</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>
```

---

## Key Design Decisions

| Decision | Choice | Rationale |
|---|---|---|
| Frontend | Thymeleaf SSR + vanilla JS | Same as `med-expert-match-ce`; no SPA build step, simpler deployment |
| Streaming | SSE (Server-Sent Events) | Native browser support; `SseEmitter` with 120s timeout |
| Chat IDs | 24-char hex (ObjectId format) | Same as `med-expert-match-ce`; URL-safe, sortable by time |
| Modularity | Spring Modulith package modules | Same pattern as `med-expert-match-ce`; `verify()` in CI |
| API surface | Interface in `service/` / `repository/` | Impl in `*/impl/`; REST/web layer never touches JDBC |
| Persistence | JDBC only (`NamedParameterJdbcTemplate`) | Consistent with `med-expert-match-ce`; no JPA/Hibernate |
| SQL externalization | `@InjectSql` + classpath SQL files | Same pattern as `med-expert-match-ce`; SQL reviewable independently |
| LLM client | Spring AI OpenAI-compatible (`OpenAiChatModel`) | Same as med-expert-match-ce; not OpenAI cloud-specific |
| LLM backend (default) | Ollama `http://localhost:11434/v1` | Local dev default; any OpenAI-compatible API via env override |
| ChatModel creation | Manual `OpenAiChatModel` via `OpenAiChatModelFactory` | Auto-config excluded; multi-role with lazy init |
| Session memory | Spring AI Session JDBC (community) | Turn-safe compaction; JTokkit token estimation |
| MCP optional at runtime | Graceful degradation; no startup failure | Chat is usable without MCP; enrichment is best-effort only |
| MCP transport | SSE (SYNC) via `McpSyncClient` | Same as ai-architect-6-mcp client config |
| MCP tool discovery | Startup initialization + `MCPToolAdvisor` | Tools available to LLM on every request |
| Harness | Generic `ChatWorkflowEngine` ported from reference state machine | Structured workflow with planning, verification, policy gates |
| Activity bridge | `ChatStreamActivityPublisher` (from reference) | Spring events → SSE `activity` events per session |
| Agent progress | SSE `activity` + `pipeline_stage` + `agent` events | Same event types as med-expert-match-ce `chat.js` |
| Security default | Permit-all for local/dev | Same as `med-expert-match-ce` local profile |
| Structured Output | Spring AI typed response records | JSON schema-constrained generation for tool calls |
| No graph DB | PostgreSQL only (no Apache AGE) | Chat app doesn't need graph operations |
| No vector DB | No pgvector extension | Chat app doesn't do semantic search (MCP server handles it) |

---

## Data Flow: Chat Message Lifecycle

```
User types message in browser
        │
        ▼
POST /api/v1/chats/{chatId}/messages/stream  (SSE)
        │
        ▼
ChatController.streamMessage()
        │
        ▼
ChatAssistantServiceImpl.streamMessage()
        │
        ├─ 0. Register SseEmitter in ChatStreamActivityPublisher (sessionId)
        ├─ 1. Sanitize user content
        ├─ 2. Append user message to DB (chatService.appendUserMessage)
        ├─ 3. Update session memory (sessionService.appendMessage)
        ├─ 4. ChatWorkflowEngine.execute() — emit pipeline_stage events
        ├─ 5. Build advisor chain:
        │      DateTimeContextAdvisor → MCPToolAdvisor → ToolCallingAdvisor
        │      → SessionMemoryAdvisor → SimpleLoggerAdvisor
        ├─ 6. Invoke ChatClient (gemma4:31b-cloud).prompt().advisors().stream()
        │      │
        │      ├─ ToolCallingAdvisor invokes functiongemma for MCP tools
        │      │   └─ ChatStreamActivityPublisher → SSE activity/tool_call
        │      ├─ LLM streams tokens
        │      │   └─ Emit SSE: token {"t":"..."}
        │      └─ Harness tracks stages via pipeline_stage events
        ├─ 7. Collect full response
        ├─ 8. Append assistant message to DB
        ├─ 9. Emit SSE: done {"id","content"}
        ├─ 10. Unregister SseEmitter
        └─ 11. Complete SseEmitter
```

---

## Related documentation

- [README.md](README.md) — documentation index and naming
- [../README.md](../README.md) — project overview
- [01-requirements.md](01-requirements.md) — SRS with goals, MCP surface, milestones
- [03-design.md](03-design.md) — schema, services, MCP client, harness class sketches
- [05-deployment.md](05-deployment.md) — config, Docker, env vars
- [04-testing.md](04-testing.md) — test pyramid and quality gates
