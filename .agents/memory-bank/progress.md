# Progress log

## 2026-06-17 — Documentation baseline

- Full `docs/` set; OpenAI-compatible client + default Ollama
- Commits on `develop`

## 2026-06-17 — AI context bootstrap

- `AGENTS.md`, `.agents/skills/`, memory bank, `docs/ai-context-strategy.md`
- Base package `com.berdachuk.aichat` (DEC-006)

## 2026-06-17 — M1 Modulith foundation

- `pom.xml`, `AiChatApplication`, Flyway `V1__init_chat_schema.sql`
- Domain `Chat`, `ChatMessage`; `IdGenerator`; six module `package-info.java`
- Tests: `ModulithArchitectureTest`, `IdGeneratorTest`, `FlywaySchemaIntegrationTest`
- **Commands:** `mvn test`, `mvn verify -Pintegration` — pass
- **Plan:** M-02 archived; M-03 active for M2

## 2026-06-17 — M3 LLM integration + SSE

- Spring AI 2.0 OpenAI-compatible client (`OpenAiChatModelFactory`, `AiChatProperties`, `SpringAIConfig`)
- `ChatAssistantService` + `ChatStreamController` — SSE `token`/`done`/`error`
- Test profile stub `ChatModel`; `ChatStreamControllerIntegrationTest`
- **Plan:** M-04 archived; M-05 active for M4 session memory

## 2026-06-17 — M4 Session memory + advisors

- `spring-ai-starter-session-jdbc` 0.3.0, Flyway `V2__init_session_schema.sql`
- `DateTimeContextAdvisor`, `SessionMemoryAdvisor`, turn-window compaction
- Session ID `{userId}-{chatId}`; default DB port `5437`
- **Plan:** M-05 archived; M-06 active for M5 harness

## 2026-06-17 — M5 Harness workflow engine

- `ChatWorkflowEngine`, `ChatStreamActivityPublisher`, stub planner/verifier/policy
- Flyway `V3__init_harness_schema.sql`, JDBC run/trace stores
- SSE `agent`, `pipeline_stage`, `activity` on stream endpoint
- **Plan:** M-06 archived; M-07 active for M6 Thymeleaf UI

## 2026-06-17 — M6 Thymeleaf chat UI

- `ChatWebController`, `chat.html`, `chat.css`, `chat.js` (SSE + agent panel)
- Sidebar CRUD via REST; `ChatPageIntegrationTest`
- **Plan:** M-07 archived; M-08 active for M7 MCP client

## 2026-06-17 — M7 MCP client + runtime catalog

- Flyway `V4__init_mcp_schema.sql` — `mcp_connection`, `chat.enabled_mcp_connections`
- `McpServerRegistry`, `McpClientConnector`, REST `/api/v1/mcp/connections`
- `MCPToolAdvisor` + `ToolCallingAdvisor` in `LlmChatClientConfiguration`
- `PUT/GET /api/v1/chats/{chatId}/mcp`; `spring-ai-starter-mcp-client` 2.0.0
- Tests: `McpServerRegistryTest`, `MCPToolAdvisorTest`, `McpConnectionControllerIntegrationTest`, `McpClientIntegrationTest`
- **Plan:** M-08 archived; M-09 active for M8 per-chat MCP UI

