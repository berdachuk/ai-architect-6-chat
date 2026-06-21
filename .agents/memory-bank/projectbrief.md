# Project brief — ai-architect-6-chat

## Identity

| Field | Value |
|---|---|
| Repo | `ai-architect-6-chat` |
| Application | `ai-chat` |
| Status | v1.0.0 released; implementation complete through M-17 |
| Branch | `develop` |
| Owner | Siarhei Berdachuk |

## Purpose

Build a general-purpose AI chat application: multi-session history, SSE streaming, long-dialog memory, Harness agent progress, optional MCP tool enrichment.

## Reference implementations

- [med-expert-match-ce](https://github.com/berdachuk/med-expert-match-ce) — chat, harness, session memory patterns
- [ai-architect-6-mcp](https://github.com/berdachuk/ai-architect-6-mcp) — optional MCP backend (`:8092/sse`)

## Goals (summary)

- Spring Modulith single deployable; JDBC only; Thymeleaf SSR
- OpenAI-compatible client (`OpenAiChatModel`); **default Ollama**
- **Chat works without MCP** at all times (NFR-001)
- Port chat subsystem from med-expert-match-ce; add MCP client (new)

## Scope

Milestones M1–M9 per [docs/01-requirements.md §14](../../docs/01-requirements.md).

## Out of scope

Medical domain, graph DB, GraphRAG, vector search in chat app, evaluation framework, MCP server role.

## Canonical docs

[docs/README.md](../../docs/README.md)