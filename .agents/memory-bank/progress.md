# Progress log

## 2026-06-18 — Developer ergonomics and chat UI polish

- `docker-compose.dev.yml` + `application-dev.yml` — local dev workflow
  (Postgres on host:5437, app via `mvn spring-boot:run -Pdev`)
- `AiChatApplication` — log UI/Health/Swagger URLs on startup
- `LlmConnectionsHealthIndicator` — per-role LLM health under
  `/actuator/health` (chat, chat-alt, tool-calling)
- `IdGenerator` — MongoDB ObjectId layout (4-byte ts + 5-byte random
  + 3-byte counter) with a timestamp-decoding test
- Chat UI: stable two-section flex layout, per-message collapsible
  agent panel, full markdown rendering with `highlight.js`
- `feature/dev-improvements` merged into `develop` and deleted

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

## 2026-06-17 — M8 Per-chat MCP + UI toggles

- Sidebar MCP panel in `chat.html` / `chat.js` with per-chat toggles
- `ActivityReportingToolCallback` publishes `tool_call` SSE activity events
- `McpBootstrapSeeder` seeds `medical-dataset` from `MCP_MEDICAL_URL` when catalog empty
- Tests: `ChatMcpSelectionIntegrationTest`, `McpBootstrapSeederTest`, `ActivityReportingToolCallbackTest`
- **Plan:** M-09 archived; M-10 active for M9 Docker + polish

## 2026-06-17 — M9 Docker + polish

- Multi-stage `Dockerfile`, `docker-compose.yml`, optional `docker-compose.mcp-host.yml`
- GitHub Actions CI (`.github/workflows/ci.yml`)
- `McpConnectionHealthIndicator` + `spring-boot-starter-actuator`
- README Docker quick-start; requirements M9 ✅
- Tests: `McpConnectionHealthIndicatorTest`, `ActuatorHealthIntegrationTest`
- **Plan:** M-10 archived; M-11 active for release/maintenance

## 2026-06-17 — M-11 Release hardening (v1.0.0)

- Actuator `show-details: never` by default; `application-prod.yml` + test profile overrides
- `SecurityConfig` — lock down non-public endpoints (`.authenticated()`)
- `SmokeChecklistIntegrationTest` + `scripts/smoke-rest.sh`
- `docs/05-deployment.md` aligned with `OLLAMA_*` env vars
- Version `1.0.0`; git tag `v1.0.0`
- **Plan:** M-11 archived; M-12 active for ongoing maintenance

## 2026-06-17 — M-12 OAuth2, user guide, observability

- Optional OAuth2/JWT (`application-oauth2.yml`); default `oauth2-enabled: false` for open dev testing
- `docs/user-guide.md` end-user documentation
- Prometheus `/actuator/prometheus` in `prod` profile
- README aligned with `OLLAMA_*` defaults (`gemma4:31b-cloud`)
- **Plan:** M-12 archived; M-13 active for E2E and Grafana

## 2026-06-17 — M-13 E2E and observability

- Playwright suite in `e2e/` with `chat-ui.spec.ts`
- CI `e2e` job: `docker compose` + Playwright against `:8095`
- Grafana dashboard `observability/grafana/ai-chat-overview.json`
- `docker-compose.e2e.yml` disables MCP bootstrap for CI
- **Plan:** M-13 archived; M-14 active for production hardening

## 2026-06-17 — M-14 Production hardening

- `oauth2-login` profile + `UserContext` OIDC principal; web UI skips `X-User-Id` when enabled
- `e2e` profile stub LLM + Playwright `chat-stream.spec.ts`
- Prometheus alert rules, MIT LICENSE, RELEASE.md
- README sync
- **Plan:** M-14 archived; M-15 active

## 2026-06-18 — M-16 MCP self-description protocol

- `McpServerInfo`: added `instructions` field
- `McpClientConnector`: capture `client.getServerInstructions()` after initialize
- `McpServerRegistry`: include server instructions + prompts with argument schemas in catalog text
- `MCPToolAdvisor`: fix `ClassCastException` — use `tcc.mutate()` instead of `DefaultToolCallingChatOptions`
- `application-debug.yml`: verbose logging profile with file output
- Tests: updated all `McpServerInfo` constructors
- Docs: `docs/mcp-self-description-improvements.md`
- **Plan:** M-16 archived

