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
