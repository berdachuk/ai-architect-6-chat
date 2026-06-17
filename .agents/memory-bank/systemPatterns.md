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
| `mcp` | MCP client, registry, tool wrappers | `McpServerInfo` (runtime) | `core` |
| `web` | Thymeleaf SSR, static JS/CSS | — | `core`, `chat`, `llm` |
| `system` | Actuator health (MCP) | — | `core`, `mcp` |

## Integration patterns

- **Persistence:** Flyway + externalized SQL; schema `ai_chat`
- **LLM:** Manual `OpenAiChatModel`; auto-config excluded; default Ollama `/v1`
- **MCP:** SSE `McpSyncClient`; per-connection try/catch; registry marks DOWN
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
- `pom.xml` on Boot 3.4.4 / Modulith 1.3.4 (DEC-010); docs target 4.1 / 2.1

Canonical: [docs/02-architecture.md](../../docs/02-architecture.md)
