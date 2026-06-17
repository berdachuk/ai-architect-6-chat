# M-07 — M6 Thymeleaf Chat UI

**Status:** Active  
**Date:** 2026-06-17  
**Milestone:** M6 per [docs/01-requirements.md §14](../../docs/01-requirements.md)

## Goal

Thymeleaf SSR chat UI with sidebar, composer, agent progress panel, and SSE client.

## Deliverables

- [ ] `web/` module: `ChatPageController`, `chat.html`, static `chat.js` / `chat.css`
- [ ] Sidebar: session list, create/rename/delete, switch chat
- [ ] Composer + SSE stream consumer (`token`, `done`, `activity`, `pipeline_stage`, `agent`)
- [ ] Agent progress panel wired to harness SSE events
- [ ] Security: permit static assets + `/chat` paths
- [ ] IT or smoke: page loads, stream renders assistant message (optional Playwright later)

## Out of scope (M7+)

- MCP connection picker UI (M7)
- MCP client runtime (M7)
