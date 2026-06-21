# System patterns

## Architecture style

Single Maven module, **Spring Modulith package modules** (`@ApplicationModule`), base package `com.berdachuk.aichat`.

## Module diagram

```text
web ──► chat, llm, core
llm ──► chat, mcp, core
chat ──► core
mcp  ──► core
system ──► core, mcp
```

## Module ownership

| Module | Responsibilities | Domain models | Depends on |
|---|---|---|---|
| `core` | Config, security, `OpenAiChatModelFactory`, `IdGenerator`, `@InjectSql` | — | — |
| `chat` | Session CRUD, messages, REST `/api/v1/chats` | `Chat`, `ChatMessage` | `core` |
| `llm` | Streaming, harness, advisors, activity publisher | harness records (V2 tables) | `core`, `chat`, `mcp` |
| `mcp` | MCP client, runtime catalog, registry, tool wrappers | `McpConnection`, `McpServerInfo` | `core` |
| `web` | Thymeleaf SSR, static JS/CSS | — | `core`, `chat`, `llm` |
| `system` | Actuator health (MCP) | — | `core`, `mcp` |

## Domain ownership & traceability

- `Chat`, `ChatMessage` → `chat` (REQ-001)
- Harness runtime → `llm` (REQ-004); `HarnessWorkflowRun`
- `McpConnection`, `McpServerInfo` → `mcp` (REQ-005)
- Executable specs: planned Cucumber IT per [docs/04-testing.md](../../docs/04-testing.md). **No `.feature` files yet** (SCN-001..002 are `Planned`, not yet wired to Gherkin).
- Requirement IDs: provisional `REQ-001..006` in `registry/req.jsonl` summarize milestone behavior; formal SRS `REQ-###` table not yet created in `docs/01-requirements.md` — flag for human review.

## Integration patterns

- **Persistence:** Flyway + externalized SQL in `sql/`; `@InjectSql`; named parameters only (DEC-013)
- **LLM:** Manual `OpenAiChatModel`; auto-config excluded; default Ollama `/v1` (DEC-003)
- **MCP:** runtime catalog (`mcp_connection`); per-chat `enabledMcpConnections` (DEC-011); optional invariant NFR-001 (DEC-002)
- **SSE contract:** `token`, `done`, `agent`, `activity`, `pipeline_stage`
- **Security:** `X-User-Id` validation NFR-003 (RISK-001); MCP URL SSRF prevention NFR-004 (RISK-002)

## Traceability gaps

- SCN-001..002 are `Planned` — no `.feature` files exist yet.
- `STEP-###` reusable step concepts referenced in `scn.jsonl` are placeholders until Cucumber is adopted.
- `TEST-001..002` cover SCN-001..002 at integration level only.

## Gaps

- Formal `REQ-###` table in `docs/01-requirements.md`: **not yet created** — provisional IDs in registry until requirements-modeling skill adds formal IDs.
- Harness tables in `V2__harness_schema.sql` — documented, not migrated (legacy note; verify current state in code).

Canonical: [docs/02-architecture.md](../../docs/02-architecture.md)