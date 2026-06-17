# Decisions (ADR log)

## DEC-001 — Spring Modulith package modules

| Field | Value |
|---|---|
| Status | Accepted |
| Date | 2026-06-17 |
| Modules | `core`, `chat`, `llm`, `mcp`, `web`, `system` |
| Rationale | Match med-expert-match-ce; `verify()` in CI |
| Consequence | No cross-module `impl` imports |

## DEC-002 — MCP optional at runtime

| Field | Value |
|---|---|
| Status | Accepted |
| Date | 2026-06-17 |
| Rationale | Core chat must work without ai-architect-6-mcp |
| Consequence | `McpClientConfig` try/catch per connection; `MCPToolAdvisor` no-op when no UP servers |

## DEC-003 — OpenAI-compatible client, default Ollama

| Field | Value |
|---|---|
| Status | Accepted |
| Date | 2026-06-17 |
| Rationale | Same as reference; swappable via env per role |
| Consequence | Exclude OpenAI auto-config; `OpenAiChatModelFactory` manual wiring |

## DEC-004 — Split chat vs tool-calling models

| Field | Value |
|---|---|
| Status | Accepted |
| Date | 2026-06-17 |
| Modules | `llm`, `core` |
| Rationale | gemma4 for chat; functiongemma for `ToolCallingAdvisor` |
| Consequence | Three `ChatClient` beans (chat, chat-alt, tool-calling) |

## DEC-005 — AI context layout

| Field | Value |
|---|---|
| Status | Accepted |
| Date | 2026-06-17 |
| Rationale | Standard agent bootstrap pattern |
| Consequence | Canonical skills in `.agents/skills/`; memory in `.agents/memory-bank/`; deep specs stay in `docs/` |

## DEC-006 — Base package `com.berdachuk.aichat`

| Field | Value |
|---|---|
| Status | Accepted |
| Date | 2026-06-17 |
| Modules | all |
| Rationale | Align with `com.berdachuk.medexpertmatch`; organization namespace `com.berdachuk` |
| Consequence | All Java sources under `com.berdachuk.aichat.*`; no `com.example` placeholders |

## DEC-007 — OpenAPI REST contract + generated clients

| Field | Value |
|---|---|
| Status | Accepted |
| Date | 2026-06-17 |
| Modules | `chat`, `web`, tests |
| Rationale | Single canonical API contract; avoid drift between docs, tests, and UI |
| Consequence | springdoc publishes spec; IT and `chat.js` CRUD use OpenAPI Generator clients from JSON/YAML |

## DEC-008 — WSL for Docker on Windows (development)

| Field | Value |
|---|---|
| Status | Accepted |
| Date | 2026-06-17 |
| Scope | Local development on Windows |
| Rationale | Docker + Testcontainers reliability on WSL 2 |
| Consequence | Run Maven/Docker from WSL; see [docs/05-deployment.md](../../docs/05-deployment.md) |

## DEC-009 — Testcontainers for integration tests

| Field | Value |
|---|---|
| Status | Accepted |
| Date | 2026-06-17 |
| Modules | tests |
| Rationale | Isolated, reproducible IT |
| Consequence | `FlywaySchemaIntegrationTest` and future IT use `postgres:17` Testcontainers; `mvn verify -Pintegration` |

## DEC-010 — M1 stack versions (pragmatic)

| Field | Value |
|---|---|
| Status | **Superseded** by DEC-012 |
| Date | 2026-06-17 |
| Note | Temporary Boot 3.4.4 used only for initial M1 scaffold |

## DEC-012 — Latest stable stack: Boot 4.x + Spring AI 2.0.0

| Field | Value |
|---|---|
| Status | Accepted |
| Date | 2026-06-17 |
| Modules | all (`pom.xml`) |
| Rationale | Stay on supported Spring generation; Spring AI 2.0 aligns with Boot 4 |
| Consequence | `spring-boot-starter-parent` **4.1.0**; `spring-ai-bom` **2.0.0**; Modulith **2.1.0**; bump to latest stable 4.x / 2.0.x patches per §11.1 |

## DEC-013 — Externalized SQL + named bind parameters

| Field | Value |
|---|---|
| Status | Accepted |
| Date | 2026-06-17 |
| Modules | `core`, `chat`, any JDBC repository |
| Rationale | Reviewable SQL, no string literals in Java, safe parameter binding; proven in [ai-architect-6-mcp](https://github.com/berdachuk/ai-architect-6-mcp/blob/main/src/main/java/com/example/medicalmcp/medicalcase/repository/impl/MedicalCaseRepositoryImpl.java) |
| Consequence | `@InjectSql` + files under `src/main/resources/sql/`; `NamedParameterJdbcTemplate` with `:name` binds only; no inline SQL, no `?` positional JDBC in app code |

## DEC-011 — Runtime MCP catalog and per-chat selection

| Field | Value |
|---|---|
| Status | Accepted |
| Date | 2026-06-17 |
| Modules | `mcp`, `chat`, `web`, `llm` |
| Rationale | Operators and users must add MCP servers and scope them per chat without redeploy |
| Consequence | Persistent `mcp_connection` catalog; `enabledMcpConnections` per chat; REST + UI; `MCPToolAdvisor` filters by chat selection (REQ-MCP-09–14) |
