# AGENTS.md — `llm` module

**Package:** `com.berdachuk.aichat.llm`  
**Depends on:** `core`, `chat`, `mcp`  
**Canonical design:** [docs/03-design.md](../../../../../../../docs/03-design.md) (ChatAssistantService, Harness, advisors)

## Purpose

LLM orchestration: streaming chat responses, advisor chain, Harness workflow engine, activity SSE publisher. Does not own chat persistence (see `chat/`).

## Domain / runtime artifacts

| Artifact | Owner notes |
|---|---|
| `ChatAssistantService` | Main streaming entry point |
| `ChatStreamActivityPublisher` | SSE `activity`, `pipeline_stage`, `agent` events |
| Harness run/trace stores | V2 schema; generic `ChatWorkflowEngine` |
| Session advisors | `SessionMemoryAdvisor`, `MCPToolAdvisor` (no-op when MCP down) |

## Advisor chain (order)

1. `DateTimeContextAdvisor` (`core`)
2. `MCPToolAdvisor` (no-op if no MCP servers UP)
3. `ToolCallingAdvisor` (`conversationHistoryEnabled: false`)
4. `SessionMemoryAdvisor` (JDBC, turn window)
5. `SimpleLoggerAdvisor`

## SSE contract

| Event | Payload |
|---|---|
| `token` | `{"t":"<chunk>"}` |
| `done` | `{id, content}` |
| `activity` | harness / tool progress |

## Boundaries

| | |
|---|---|
| ✅ | Stream responses, harness, advisors, activity events |
| 🚫 | Direct Thymeleaf, REST chat CRUD (use `chat/`, `web/`) |
| 🚫 | MCP client wiring (use `mcp/` registry only) |

## Skills

- [core-architecture](../../../../../../../.agents/skills/core-architecture/SKILL.md)
- [testing](../../../../../../../.agents/skills/testing/SKILL.md)
- [security-check](../../../../../../../.agents/skills/security-check/SKILL.md) — LLM input/output, tool calls
