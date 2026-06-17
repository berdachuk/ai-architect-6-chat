# M-09 — M8 Per-Chat MCP + UI Toggles

**Status:** Active  
**Date:** 2026-06-17  
**Milestone:** M8 per [docs/01-requirements.md §14](../../docs/01-requirements.md)

## Goal

Per-chat MCP selection in the UI and integration with ai-architect-6-mcp when available.

## Deliverables

- [ ] Thymeleaf MCP panel — list catalog connections, toggle per chat
- [ ] `chat.js` — `GET/PUT /api/v1/chats/{chatId}/mcp` wiring
- [ ] Activity panel shows MCP tool calls when advisor invokes tools
- [ ] Bootstrap seed for `medical-dataset` (`:8092/sse`) via env on first deploy
- [ ] IT: enable connection for chat → `MCPToolAdvisor` scopes tools
- [ ] Manual smoke: ai-architect-6-mcp connected → tools in chat turn

## Out of scope (M9+)

- MCP health actuator indicator (`system/` module)
- Full deployment / CI hardening
