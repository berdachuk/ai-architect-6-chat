# Testing Strategy — `ai-chat`

**Version:** 1.0.0
**Date:** 2026-06-17
**Related:** [01-requirements.md](01-requirements.md) · [02-architecture.md](02-architecture.md) · [03-design.md](03-design.md)

---

## 1. Goals

| Goal | How |
|---|---|
| **Correctness** | Chat CRUD, message persistence, session memory; **chat streams without MCP** |
| **Contract** | REST API shapes stable, SSE event format consistent |
| **Integration** | MCP client connects to ai-architect-6-mcp (`medical-mcp-server`), tools are callable |
| **Modulith boundaries** | `ApplicationModules.of(...).verify()` passes — no illegal cross-module deps |
| **Streaming** | SSE token delivery, agent activity events, completion signal |
| **Regression** | Modulith verification on every build |

**Non-goals:** Testing LLM output quality (non-deterministic). Testing Ollama availability (external dependency). Testing MCP server correctness (belongs to [ai-architect-6-mcp](https://github.com/berdachuk/ai-architect-6-mcp) test suite).

---

## 2. Test pyramid

```text
                     ┌─────────────────────┐
                     │  E2E / Manual smoke │  Browser + Ollama + MCP server
                     │  (few, manual)      │
                     └──────────┬──────────┘
                ┌───────────────┴───────────────┐
                │  Integration (Testcontainers) │  PG, MCP client (WireMock), SSE
                │  @SpringBootTest              │
                │  @Tag("integration")          │
                └───────────────┬───────────────┘
      ┌─────────────────────────┴─────────────────────────┐
      │  Unit + Modulith                                   │  Fast, every commit
      │  ModulithArchitectureTest, service/repo unit tests │
      └────────────────────────────────────────────────────┘
```

### Maven profiles

| Profile | Tests | When |
|---|---|---|
| default | unit + modulith | Every `mvn test` |
| `integration` | + Testcontainers IT | Every PR (`mvn verify -Pintegration`) |
| `e2e` | + live Ollama + MCP server | Manual / staging |

---

## 3. Test layout

```text
src/test/java/com/berdachuk/aichat/
├── ModulithArchitectureTest.java
├── chat/
│   ├── domain/
│   │   └── ChatMessageTest.java              # JSON serialization, role constants
│   ├── repository/
│   │   └── ChatRepositoryImplTest.java       # @JdbcTest with H2 or mocked JdbcTemplate
│   └── service/
│       └── ChatServiceImplTest.java          # Mock repositories, verify logic
├── llm/
│   ├── harness/
│   │   ├── AgentPlannerServiceImplTest.java  # Plan parsing from JSON
│   │   └── AgentResponseVerifierTest.java    # Verification logic
│   └── advisor/
│       └── MCPToolAdvisorTest.java           # Tool definition injection
├── mcp/
│   └── registry/
│       └── McpServerRegistryTest.java          # Tool callback wrapping, catalog text
└── integration/
    ├── AbstractPostgresIntegrationTest.java   # @Testcontainers PG 17
    ├── FlywaySchemaIntegrationTest.java       # Schema creation, indexes
    ├── ChatControllerIntegrationTest.java    # REST endpoints with real DB
    ├── ChatStreamingIntegrationTest.java     # SSE streaming with mocked LLM
    └── McpClientIntegrationTest.java         # MCP client with WireMock MCP server

src/test/resources/
├── application-test.yml                       # Test profile config
└── sql/                                       # Test fixture SQL
```

---

## 4. Layer-by-layer approach

### 4.1 Unit tests (fast)

| Component | What to test |
|---|---|
| `IdGenerator.generateId()` | Returns 24-char hex, unique across 10K calls |
| `ChatMessage` record | JSON serialization/deserialization round-trip |
| `ChatRepositoryImpl` | SQL mapping with mocked `NamedParameterJdbcTemplate` |
| `ChatServiceImpl` | Default chat creation, delete + recreate logic, auto-naming from first message |
| `AgentPlannerServiceImpl` | Parses valid JSON plan, handles malformed JSON gracefully |
| `AgentResponseVerifier` | Detects missing tool outputs, validates against acceptance criteria |
| `McpServerRegistry` | Wraps MCP tool definitions as ToolCallback, formats tool catalog for LLM |
| `MCPToolAdvisor` | Injects tool catalog into system text, provides tool callbacks; no-op when no servers UP |
| `OpenAiChatModelFactory` | URL normalization (appends `/v1`), model options mapping |

### 4.2 Modulith boundary test

```java
@ApplicationModuleTest
class ModulithArchitectureTest {
    @Test
    void verifyModuleBoundaries() {
        ApplicationModules.of(AiChatApplication.class).verify();
    }
}
```

Runs on every `mvn test` — blocks illegal cross-module dependencies.

### 4.3 Integration tests (Testcontainers)

**Infrastructure:** `postgres:17` container, Flyway `V1__init_chat_schema.sql`.

| Test class | Verifies |
|---|---|
| `FlywaySchemaIntegrationTest` | Schema `ai_chat` exists; `V1` chat tables + indexes; `V2` harness tables (M5+) |
| `ChatControllerIntegrationTest` | Full CRUD cycle: create → list → get history → rename → delete → default recreation |
| `ChatStreamingIntegrationTest` | SSE endpoint returns `text/event-stream`, token/done events; mocked ChatClient; **passes with `ai-chat.features.mcp-client: false`** |
| `McpUnavailableStartupTest` | App context loads when MCP URL points to dead host; chat stream endpoint still responds |
| `McpClientIntegrationTest` | WireMock MCP server → `McpSyncClient` initialization → tool listing → tool calling |

### 4.4 REST API contract tests

| Endpoint | Contract assertions |
|---|---|
| `GET /api/v1/chats` | Array of objects with `id`, `userId`, `name`, `agentId`, `isDefault`, `messageCount`, timestamps |
| `POST /api/v1/chats` | Returns created chat; first chat has `isDefault: true` |
| `GET /api/v1/chats/{chatId}/history` | Array of messages with `id`, `chatId`, `role`, `content`, `sequenceNumber`; ordered by seq ASC |
| `DELETE /api/v1/chats/{chatId}` | 204 No Content; chat removed from list; default recreated if last chat |
| `PUT /api/v1/chats/{chatId}/name` | Name updated; 200 OK |
| `POST /api/v1/chats/{chatId}/messages/stream` | `Content-Type: text/event-stream`; events: `token`, `done` |

**Negative cases:**

- Invalid chatId → 404
- ChatId belonging to different user → 404 (via `requireOwnedChat`)
- Missing `X-User-Id` header → falls back to "anonymous" user

### 4.5 SSE streaming contract

| Event | Data shape |
|---|---|
| `token` | `{"t":"<chunk>"}` (JSON) |
| `activity` | `{"type":"tool_call","toolName":"...","message":"..."}` |
| `agent` | `{"type":"agent_start","agentId":"..."}` or `{"type":"agent_done",...}` |
| `pipeline_stage` | `{"stage":"PLANNING","agent":"...","status":"...","timestampMs":N}` |
| `done` | `{"id":"...","content":"..."}` |

### 4.6 MCP client integration (WireMock)

```java
@Tag("integration")
@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
class McpClientIntegrationTest {

    @DynamicPropertySource
    static void overrideMcpUrl(DynamicPropertyRegistry registry) {
        registry.add("spring.ai.mcp.client.sse.connections.test-server.url",
            () -> wireMockServer.baseUrl() + "/sse");
    }

    @Test
    void shouldDiscoverAndCallMcpTools() {
        // WireMock stubs MCP SSE handshake + tool list + tool call
        // Verify McpServerRegistry returns tool callbacks
        // Verify tool call through McpSyncClient returns expected result
    }
}
```

---

## 5. Test fixtures

### SQL fixtures (`src/test/resources/sql/`)

```sql
-- test-fixtures.sql
INSERT INTO ai_chat.chat (id, user_id, name, agent_id, is_default, created_at, updated_at, last_activity_at, message_count)
VALUES
('000000000000000000000001', 'test-user', 'Test Chat', 'auto', true, now(), now(), now(), 2);

INSERT INTO ai_chat.chat_message (id, chat_id, role, content, sequence_number, tokens_used, created_at)
VALUES
('000000000000000000000011', '000000000000000000000001', 'user', 'Hello', 1, 5, now()),
('000000000000000000000012', '000000000000000000000001', 'assistant', 'Hi there!', 2, 10, now());
```

---

## 6. CI pipeline

```yaml
# .github/workflows/ci.yml
name: CI
on:
  push:
    branches: [develop, main]
  pull_request:
    branches: [develop, main]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: 21
          distribution: temurin
      - name: Unit + Modulith tests
        run: mvn test -B
      - name: Integration tests
        run: mvn verify -Pintegration -B
```

---

## 7. Manual smoke checklist (M9)

Automated REST coverage: `SmokeChecklistIntegrationTest` and `scripts/smoke-rest.sh` (against a running instance).

| Check | Automated | Notes |
|---|---|---|
| App starts, health UP | ✅ IT + `smoke-rest.sh` | `GET /actuator/health` |
| Home → default chat | ✅ IT | `GET /` |
| Sidebar "New Chat" | ✅ IT | HTML assertion |
| Stream without MCP | ✅ IT | Stub LLM in test profile |
| Stream token-by-token | manual | Requires Ollama |
| Agent panel activity | manual | Browser + MCP tools |
| Create / switch / delete chat | ✅ IT | REST CRUD |
| MCP tools (list specialties, search cases) | manual | Requires ai-architect-6-mcp + Ollama |

Manual steps (browser + live Ollama/MCP):

- [ ] Type message with **no MCP server running** → streaming LLM response still works
- [ ] Type message → streaming response appears token-by-token
- [ ] Agent panel shows activity entries (if MCP tools called)
- [ ] ai-architect-6-mcp connected → tools available in chat
- [ ] Ask "list medical specialties" → `list_specialties` tool called
- [ ] Ask "search for cardiovascular cases" → `search_cases` tool called

**CLI smoke (running app):** `bash scripts/smoke-rest.sh http://localhost:8095`

---

## 8. What not to test

| Item | Reason |
|---|---|
| LLM response quality | Non-deterministic; out of scope |
| Ollama availability | External dependency; mocked in tests |
| MCP server correctness | Belongs to [ai-architect-6-mcp](https://github.com/berdachuk/ai-architect-6-mcp) test suite |
| Browser rendering | Thymeleaf SSR; manual smoke test sufficient |
| Session memory compaction | Spring AI Session JDBC library responsibility |
| Structured Output JSON schema | Spring AI library responsibility |

---

## Related documentation

- [README.md](README.md) — documentation index and naming
- [../README.md](../README.md) — project overview
- [01-requirements.md](01-requirements.md) — SRS with milestones
- [02-architecture.md](02-architecture.md) — Modulith modules and design decisions
- [03-design.md](03-design.md) — service and MCP implementation
- [05-deployment.md](05-deployment.md) — configuration and Docker
