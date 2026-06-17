# M-09 — M8 Per-Chat MCP + UI Toggles

**Status:** Completed  
**Date:** 2026-06-17  
**Milestone:** M8 per [docs/01-requirements.md §14](../../docs/01-requirements.md)

## Goal

Per-chat MCP selection in the UI and integration with ai-architect-6-mcp when available.

## Deliverables

- [x] Thymeleaf MCP panel — list catalog connections, toggle per chat
- [x] `chat.js` — `GET/PUT /api/v1/chats/{chatId}/mcp` wiring
- [x] Activity panel shows MCP tool calls when advisor invokes tools
- [x] Bootstrap seed for `medical-dataset` (`:8092/sse`) via env on first deploy
- [x] IT: enable connection for chat → `MCPToolAdvisor` scopes tools
- [x] Manual smoke: ai-architect-6-mcp connected → tools in chat turn (documented in plan M-10)

## Out of scope (M9+)

- MCP health actuator indicator (`system/` module)
- Full deployment / CI hardening
