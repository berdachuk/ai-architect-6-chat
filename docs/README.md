# AI Chat — Documentation Index

**Project:** `ai-architect-6-chat` (`ai-chat`)  
**Version:** 1.0.0  
**Date:** 2026-06-17  
**Status:** Draft — requirements complete, implementation not started

## Purpose

General-purpose AI chat with multi-session history, long-dialog memory, optional MCP enrichment, and Harness-style agent progress — built on the same Spring Modulith + Thymeleaf SSR patterns as [med-expert-match-ce](https://github.com/berdachuk/med-expert-match-ce), without graph databases, medical domain logic, or evaluation frameworks.

**Core chat does not require MCP.** MCP servers may be absent or down; the application must still start and stream responses.

## Naming

| Name | Meaning |
|---|---|
| **ai-architect-6-chat** | This Git repository |
| **ai-chat** | Application logical name (`spring.application.name`) |
| **ai-architect-6-mcp** | MCP backend Git repository |
| **medical-mcp-server** | Spring application name inside ai-architect-6-mcp (`spring.ai.mcp.server.name`) |
| **medical-dataset** | Default MCP client connection key in `application.yml` |

## Reference repositories

| Repository | Role in this project |
|---|---|
| [med-expert-match-ce](https://github.com/berdachuk/med-expert-match-ce) | **Primary pattern source** — chat CRUD, SSE streaming, session memory, Harness progress UI, LLM multi-role config, JDBC repositories |
| [ai-architect-6-mcp](https://github.com/berdachuk/ai-architect-6-mcp) | **Optional MCP backend (phase 2)** — medical dataset tools on `:8092/sse` when available |

## Reading order

| Step | Document | Contents |
|---|---|---|
| — | [01-requirements.md](01-requirements.md) | SRS — goals, sessions, MCP, Harness, LLM models, milestones |
| 1 | [02-architecture.md](02-architecture.md) | SAD — deployment topology, Modulith modules, design decisions |
| 2 | [03-design.md](03-design.md) | SDD — schema, services, MCP client, Harness, frontend |
| 3 | [04-testing.md](04-testing.md) | Test pyramid, contracts, CI gates |
| 4 | [05-deployment.md](05-deployment.md) | Config, Docker, Ollama, MCP connection |
| + | [ai-context-strategy.md](ai-context-strategy.md) | AI agent context layers, memory bank, skills |

## Implementation phases

| Phase | Scope | Milestones |
|---|---|---|
| **Phase 1 — Core chat** | Sessions, SSE streaming, session memory, Harness UI, LLM wiring (no MCP) | M1–M6 |
| **Phase 2 — MCP enrichment** | MCP client, tool discovery, [ai-architect-6-mcp](https://github.com/berdachuk/ai-architect-6-mcp) integration | M7–M8 |
| **Phase 3 — Packaging** | Docker, integration tests, smoke checklist | M9 |

Phase 2 does not block phase 1. **The chat must work end-to-end without any MCP server** — MCP is enrichment only, never a startup or runtime hard dependency.

## What is ported vs new

| From med-expert-match-ce | New in ai-chat |
|---|---|
| `chat/` module (CRUD, JDBC, REST) | `mcp/` module (MCP **client**) |
| `ChatStreamActivityPublisher` pattern | `ChatWorkflowEngine` (generic Harness, not medical engines) |
| `SessionMemoryAdvisor` + JDBC compaction | `MCPToolAdvisor` + `McpServerRegistry` |
| `chat.js` agent progress panel | Automatic MCP server/tool selection per turn |
| `OpenAiChatModelFactory` + `spring.ai.custom.*` | — |
| Medical tools, GraphRAG, harness engines | — (explicitly excluded) |

## Development pipeline

```text
Requirements (01) → Architecture (02) → Design (03) → Tests (04) → Deploy (05) → Implement
```

Track milestone status in [01-requirements.md §14](01-requirements.md#14-milestones).
