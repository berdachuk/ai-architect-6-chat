# Product context

## User-facing capabilities (planned)

| Capability | Description |
|---|---|
| Multi-session chat | Create, list, rename, delete sessions; one default per user |
| Streaming replies | Token-by-token SSE; Markdown rendering |
| Long dialogs | Session JDBC compaction (20 turns / 4000 tokens) |
| Agent progress | Collapsible panel: tool calls, stages, reasoning |
| MCP enrichment | Optional — external tools when servers UP |

## Users (dev phase)

Local/dev users via `X-User-Id` header or `aichat-user-id` cookie (default `anonymous`).

## Constraints

- Port `8080`; PostgreSQL `ai_chat` schema
- No API key auth in local profile
- MCP optional — never block core chat

## Non-goals

Medical matching, document ingestion, own MCP server, SPA frontend.

## UX reference

med-expert-match-ce `chat.html` / `chat.js` minus medical explainability panels.

Details: [docs/01-requirements.md §3–8](../../docs/01-requirements.md)
