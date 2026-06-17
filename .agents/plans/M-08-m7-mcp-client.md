# M-08 — M7 MCP Client + Runtime Catalog

**Status:** Active  
**Date:** 2026-06-17  
**Milestone:** M7 per [docs/01-requirements.md §14](../../docs/01-requirements.md)

## Goal

Optional MCP client with runtime connection catalog and scoped tool injection.

## Deliverables

- [ ] `mcp_connection` Flyway schema + JDBC repository
- [ ] `McpServerRegistry`, `McpClientConfig` (SSE clients)
- [ ] REST `/api/v1/mcp/connections` CRUD
- [ ] `MCPToolAdvisor` (no-op when no servers UP)
- [ ] Per-chat `enabledMcpConnections` on `Chat` + `PUT /api/v1/chats/{chatId}/mcp`
- [ ] Wire tool-calling advisor chain in `LlmChatClientConfiguration`
- [ ] IT: WireMock MCP server, tool catalog registration

## Out of scope (M8+)

- MCP picker UI in Thymeleaf (can be minimal toggle in M8)
- Medical / domain-specific tools
