# Testing Strategy — `ai-chat`

**Version:** 1.0.0
**Date:** 2026-06-17
**Related:** [01-requirements.md](01-requirements.md) · [02-architecture.md](02-architecture.md) · [03-design.md](03-design.md)

---

## 1. Goals

| Goal | How |
|---|---|
| **Correctness** | Chat CRUD operations, message persistence, session memory compaction |
| **Contract** | REST API shapes stable, SSE event format consistent |
| **Integration** | MCP client connects to `medical-mcp-server`, tools are callable |
| **Modulith boundaries** | `ApplicationModules.of(...).verify()` passes — no illegal cross-module deps |
| **Streaming** | SSE token delivery, agent activity events, completion signal |
| **Regression** | Modulith verification on every build |

**Non-goals:** Testing LLM output quality (non-deterministic). Testing Ollama availability (external dependency). Testing MCP server correctness (belongs to `medical-mcp-server` test suite).

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
src/test/java/com/example/aichat/
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
│   └── tool/
│       └── McpToolProviderTest.java          # Tool callback wrapping
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
| `McpToolProvider` | Wraps MCP tool definitions as ToolCallback, formats tool text for LLM |
| `MCPToolAdvisor` | Injects tool definitions into system text, provides tool callbacks |
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
| `FlywaySchemaIntegrationTest` | Schema `ai_chat` exists, tables `chat` + `chat_message` created, indexes present, CHECK constraint on `role` |
| `ChatControllerIntegrationTest` | Full CRUD cycle: create → list → get history → rename → delete → default recreation |
| `ChatStreamingIntegrationTest` | SSE endpoint returns `text/event-stream`, token events, done event; mocked ChatClient |
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
| `token` | String (plain text or JSON `{"text": "..."}`) |
| `activity` | `{"type": "tool_call", "name": "...", "arguments": {...}}` |
| `agent` | `{"type": "agent_start", "name": "..."}` or `{"type": "agent_done", "summary": "..."}` |
| `pipeline_stage` | `{"stage": "PLANNING"}` |
| `done` | `{"messageId": "...", "tokensUsed": N}` |

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
        // Verify McpToolProvider returns tool callbacks
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

- [ ] App starts on `:8080`, health endpoint returns UP
- [ ] Open `http://localhost:8080/` → redirected to default chat
- [ ] Sidebar shows "New Chat" (default)
- [ ] Type message → streaming response appears token-by-token
- [ ] Agent panel shows activity entries (if MCP tools called)
- [ ] Create new chat → appears in sidebar
- [ ] Switch between chats → history loads correctly
- [ ] Delete chat → removed from sidebar, default recreated if last
- [ ] MCP server (`medical-mcp-server`) connected → tools available in chat
- [ ] Ask "list medical specialties" → LLM calls `list_specialties` MCP tool → response includes specialties
- [ ] Ask "search for cardiovascular cases" → LLM calls `search_cases` MCP tool → response includes cases

---

## 8. What not to test

| Item | Reason |
|---|---|
| LLM response quality | Non-deterministic; out of scope |
| Ollama availability | External dependency; mocked in tests |
| MCP server correctness | Belongs to `medical-mcp-server` test suite |
| Browser rendering | Thymeleaf SSR; manual smoke test sufficient |
| Session memory compaction | Spring AI Session JDBC library responsibility |
| Structured Output JSON schema | Spring AI library responsibility |

---

## Related documentation

- [01-requirements.md](01-requirements.md) — SRS with milestones
- [02-architecture.md](02-architecture.md) — Modulith modules and design decisions
- [03-design.md](03-design.md) — service and MCP implementation
- [05-deployment.md](05-deployment.md) — configuration and Docker
