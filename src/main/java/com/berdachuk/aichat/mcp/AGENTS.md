# AGENTS.md — `mcp` module

**Package:** `com.berdachuk.aichat.mcp`  
**Depends on:** `core` only  
**Canonical design:** [docs/03-design.md](../../../../../../../docs/03-design.md) (MCP client, registry)

## Purpose

Optional MCP client integration. **Runtime catalog** (add connections via API/UI, no redeploy). **Per-chat selection** — user enables subset of catalog for each session; `MCPToolAdvisor` filters tools accordingly.

## Functional requirements

REQ-MCP-09–14: [docs/01-requirements.md §5](../../../../../../../docs/01-requirements.md#dynamic-mcp-catalog-and-per-chat-context-functional-requirements)

## Domain / runtime artifacts

| Artifact | Role |
|---|---|
| `McpServerRegistry` | Server catalog + `ToolCallback` list (not `McpToolProvider`) |
| `McpClientConfig` | `McpSyncClient` beans; per-connection try/catch — never fail startup |
| `McpToolCallbackWrapper` | MCP tool → Spring AI `ToolCallback` |

## Invariants

- Startup succeeds with no MCP servers or all connections failing
- `MCPToolAdvisor` in `llm/` becomes no-op when registry empty / all DOWN or none selected for chat
- Bootstrap seed may include ai-architect-6-mcp `:8092/sse`; adding connections only via YAML/Java is forbidden for production (REQ-MCP-14)

## Boundaries

| | |
|---|---|
| ✅ | Client config, registry, health signal for `system/` |
| 🚫 | LLM streaming, chat CRUD, Thymeleaf |
| 🚫 | Treating MCP as required for core chat paths |

## Skills

- [security-check](../../../../../../../.agents/skills/security-check/SKILL.md) — external tool calls, SSRF, secrets in URLs
- [core-architecture](../../../../../../../.agents/skills/core-architecture/SKILL.md)
