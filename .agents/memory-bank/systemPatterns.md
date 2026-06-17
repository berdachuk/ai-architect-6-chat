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

## Integration patterns

- **Persistence:** Flyway + externalized SQL in `sql/`; `@InjectSql`; named parameters only (DEC-013)
- **LLM:** Manual `OpenAiChatModel`; auto-config excluded; default Ollama `/v1`
- **MCP:** runtime catalog (`mcp_connection`); per-chat `enabledMcpConnections`; REQ-MCP-09–14 (DEC-011)
- **SSE contract:** `token`, `done`, `agent`, `activity`, `pipeline_stage`

## Traceability (provisional)

| Milestone | Primary module | Doc reference |
|---|---|---|
| M1 | all | §14 M1 — schema, modulith |
| M2 | `chat` | §3 sessions |
| M3–M4 | `llm`, `core` | §7 LLM, §4 memory |
| M5 | `llm` | §6 harness |
| M6 | `web` | §8 frontend |
| M7–M8 | `mcp` | §5 MCP |
| M9 | infra | §15 Docker |

Formal `REQ-###` table: **not yet created** — use milestone IDs + doc sections until requirements-modeling skill adds IDs.

## Executable specs

Planned: Cucumber IT per [docs/04-testing.md](../../docs/04-testing.md). **No `.feature` files yet.**

## Gaps

- M2+ implementation in progress (see `.agents/plans/M-03-m2-chat-crud-openapi.md`)
- Harness tables in `V2__harness_schema.sql` — documented, not migrated
- `pom.xml` on Boot 4.1.0, Modulith 2.1.0, Spring AI BOM 2.0.0 (DEC-012)

Canonical: [docs/02-architecture.md](../../docs/02-architecture.md)
