# AGENTS.md — `mcp` module

**Package:** `com.berdachuk.aichat.mcp`  
**Depends on:** `core` only  
**Canonical design:** [docs/03-design.md](../../../../../../../docs/03-design.md) (MCP client, registry)

## Purpose

Optional MCP client integration. Discovers tools/resources from configured MCP servers; exposes catalog to `llm` via `McpServerRegistry`. **Chat must work when this module has zero UP servers.**

## Domain / runtime artifacts

| Artifact | Role |
|---|---|
| `McpServerRegistry` | Server catalog + `ToolCallback` list (not `McpToolProvider`) |
| `McpClientConfig` | `McpSyncClient` beans; per-connection try/catch — never fail startup |
| `McpToolCallbackWrapper` | MCP tool → Spring AI `ToolCallback` |

## Invariants

- Startup succeeds with no MCP servers or all connections failing
- `MCPToolAdvisor` in `llm/` becomes no-op when registry empty / all DOWN
- Default target: ai-architect-6-mcp `:8092/sse` (phase 2, M7–M8)

## Boundaries

| | |
|---|---|
| ✅ | Client config, registry, health signal for `system/` |
| 🚫 | LLM streaming, chat CRUD, Thymeleaf |
| 🚫 | Treating MCP as required for core chat paths |

## Skills

- [security-check](../../../../../../../.agents/skills/security-check/SKILL.md) — external tool calls, SSRF, secrets in URLs
- [core-architecture](../../../../../../../.agents/skills/core-architecture/SKILL.md)
