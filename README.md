# ai-architect-6-chat

General-purpose AI chat with multi-session history, long-dialog memory, optional MCP context enrichment, and Harness-style agent progress.

**Core chat works without MCP** — sessions, streaming, and session memory do not depend on any MCP server. MCP adds tool enrichment when available (phase 2).

Built on the chat patterns from [med-expert-match-ce](https://github.com/berdachuk/med-expert-match-ce) — without graph databases, medical domain logic, or evaluation frameworks. Connects to [ai-architect-6-mcp](https://github.com/berdachuk/ai-architect-6-mcp) for MCP tool enrichment (phase 2).

**Version:** 1.0.0 · **Status:** `develop` — M1–M9 complete, v1.0.0 release hardening

---

## Overview

`ai-chat` is a **Spring AI 2.0** application with a Thymeleaf SSR web UI. Users manage multiple chat sessions, stream assistant responses token-by-token over SSE, and see real-time agent activity (tool calls, planning steps, pipeline stages) in a collapsible progress panel.

| Property | Value |
|---|---|
| Port | `8095` |
| Transport | HTTP + SSE (`text/event-stream`) |
| Database | PostgreSQL 17 (`ai_chat` schema) |
| LLM client | Spring AI OpenAI-compatible (`OpenAiChatModel`) |
| LLM backend (default) | Ollama (`http://localhost:11434`) |
| Base package | `com.berdachuk.aichat` |
| MCP transport | SSE (`McpSyncClient`) |

---

## Features

| Feature | Description |
|---|---|
| **Multi-session chat** | Create, list, rename, delete sessions; one default chat per user |
| **Streaming responses** | SSE token streaming with Markdown rendering (`marked.js` + DOMPurify) |
| **Long dialog support** | Spring AI Session JDBC with turn-window compaction (20 turns / 4000 tokens) |
| **Harness workflow** | Generic `ChatWorkflowEngine` — planning, tool execution, verification, policy gate |
| **Agent progress UI** | Real-time SSE events: `activity`, `pipeline_stage`, `agent` (ported from med-expert-match-ce) |
| **MCP client** | Optional — runtime catalog (add servers without redeploy); per-chat MCP toggles; chat works when none selected |
| **Multi-role LLM** | Configurable via `OLLAMA_CHAT_MODEL`, `OLLAMA_CHAT_ALT_MODEL`, `OLLAMA_TOOL_MODEL` (default `gemma3:4b`) |
| **OpenAI-compatible client** | Spring AI `OpenAiChatModel`; default backend **Ollama**; swappable via env vars |
| **Spring Modulith** | Package modules with `allowedDependencies`; `verify()` in CI |

---

## Architecture

Single deployable Spring Boot app with package-based Modulith modules:

```text
src/main/java/com/berdachuk/aichat/
├── core/     # Config, security, OpenAiChatModelFactory, IdGenerator
├── chat/     # Sessions, messages, REST /api/v1/chats
├── llm/      # ChatAssistantService, Harness, SessionMemoryAdvisor, activity publisher
├── mcp/      # MCP client, McpServerRegistry, MCPToolAdvisor
├── web/      # Thymeleaf SSR (chat.html, chat.js)
└── system/   # Actuator health (MCP connection indicator)
```

```text
Browser (Thymeleaf + chat.js)
        │  HTTP + SSE
        ▼
   ai-chat :8095
        ├── PostgreSQL 17  (sessions, messages, ai_session)
        ├── Ollama         (default `gemma3:4b` — override via `OLLAMA_*` env vars)
        └── MCP server(s)  (ai-architect-6-mcp :8092/sse — phase 2)
```

Details: [docs/02-architecture.md](docs/02-architecture.md)

---

## Technology stack

| Component | Version |
|---|---|
| Java | 21 |
| Spring Boot | **4.1.0** (latest stable **4.x**) |
| Spring AI | **2.0.0** BOM |
| Spring Modulith | **2.1.0** |
| PostgreSQL | 17 |
| `spring-ai-starter-session-jdbc` | 0.3.0 |
| `spring-ai-starter-mcp-client` | 2.0.0 |
| `spring-ai-agent-utils` | 0.10.0 |
| Frontend | Thymeleaf 3 + Bootstrap 5.3 + vanilla JS |

---

## LLM connection

- **Client:** Spring AI Ollama provider (`OpenAiChatModel`-compatible factory)
- **Default backend:** Ollama at `http://localhost:11434`
- **Override:** `OLLAMA_BASE_URL`, `OLLAMA_CHAT_MODEL`, `OLLAMA_CHAT_ALT_MODEL`, `OLLAMA_TOOL_MODEL`

## LLM models (Ollama)

Defaults match `src/main/resources/application.yml`. Override per role with env vars.

| Role | Env var | Default model | Purpose |
|---|---|---|---|
| Primary chat | `OLLAMA_CHAT_MODEL` | `gemma3:4b` | Reasoning, streaming response |
| Alternative chat | `OLLAMA_CHAT_ALT_MODEL` | `gemma3:4b` | Lighter fallback |
| Tool calling | `OLLAMA_TOOL_MODEL` | `gemma3:4b` | MCP tool invocation (`ToolCallingAdvisor`) |

```bash
ollama pull gemma3:4b
# optional larger models:
# ollama pull gemma4:31b-cloud && export OLLAMA_CHAT_MODEL=gemma4:31b-cloud
```

---

## Reference repositories

| Repository | Role |
|---|---|
| [med-expert-match-ce](https://github.com/berdachuk/med-expert-match-ce) | Chat CRUD, SSE streaming, session memory, Harness UI, JDBC patterns |
| [ai-architect-6-mcp](https://github.com/berdachuk/ai-architect-6-mcp) | MCP backend — 5 tools, 2 resources, 1 prompt on `:8092/sse` |

**Ported from med-expert-match-ce:** `chat/` module, `ChatStreamActivityPublisher`, session compaction, `chat.js` agent panel, `OpenAiChatModelFactory`.

**New in ai-chat:** `mcp/` client module, `McpServerRegistry`, `MCPToolAdvisor`, generic `ChatWorkflowEngine`.

**Excluded:** Apache AGE, pgvector, GraphRAG, medical domain tools, evaluation framework.

---

## Implementation phases

| Phase | Milestones | Scope |
|---|---|---|
| **1 — Core chat** | M1–M6 | Schema, CRUD, LLM streaming, session memory, Harness UI — **no MCP required** |
| **2 — MCP** | M7–M8 | MCP client + [ai-architect-6-mcp](https://github.com/berdachuk/ai-architect-6-mcp) integration |
| **3 — Packaging** | M9 | Docker Compose, integration tests, smoke checklist |

Phase 2 does not block phase 1. The chat must work end-to-end without any MCP server running.

Track progress: [docs/01-requirements.md §14](docs/01-requirements.md#14-milestones)

---

## Quick start

**Prerequisites:** JDK 21, Maven 3.9+, Docker (WSL 2 on Windows per DEC-008), Ollama for LLM.

### Docker Compose (recommended)

Runs PostgreSQL + ai-chat. Ollama and optional MCP run on the host (`host.docker.internal`).

```bash
docker compose up --build
```

Open `http://localhost:8095/`. Health: `GET http://localhost:8095/actuator/health`

With [ai-architect-6-mcp](https://github.com/berdachuk/ai-architect-6-mcp) on the host (`:8092`):

```bash
docker compose -f docker-compose.yml -f docker-compose.mcp-host.yml up --build
```

Manual smoke checklist: [docs/04-testing.md §7](docs/04-testing.md#7-manual-smoke-checklist)

### Local development

```bash
mvn test                      # unit + Modulith
mvn verify -Pintegration        # + Testcontainers IT (DEC-009)

# Local run (needs PostgreSQL)
docker run -d --name ai-chat-postgres \
  -e POSTGRES_DB=ai_chat -e POSTGRES_USER=ai_chat -e POSTGRES_PASSWORD=ai_chat \
  -p 5437:5432 postgres:17
mvn spring-boot:run
```

Open `http://localhost:8095/`

**With MCP on host:**

```bash
# Terminal 1 — ai-architect-6-mcp
cd ../ai-architect-6-mcp && docker compose up -d

# Terminal 2 — ai-chat
mvn spring-boot:run
```

---

## API overview

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/v1/chats` | List sessions for current user |
| `POST` | `/api/v1/chats` | Create session |
| `GET` | `/api/v1/chats/{id}/history` | Paginated message history |
| `PUT` | `/api/v1/chats/{id}/name` | Rename session |
| `DELETE` | `/api/v1/chats/{id}` | Delete session |
| `POST` | `/api/v1/chats/{id}/messages/stream` | Send message — SSE response |

User identity: `X-User-Id` header or `aichat-user-id` cookie (dev default: `anonymous`).

Health: `GET /actuator/health`

---

## Documentation

Full index: **[docs/README.md](docs/README.md)**

| Document | Purpose |
|---|---|
| [01-requirements.md](docs/01-requirements.md) | Software requirements (SRS) — source of truth |
| [02-architecture.md](docs/02-architecture.md) | Software architecture (SAD) |
| [03-design.md](docs/03-design.md) | Detailed design (SDD) — schema, services, frontend |
| [04-testing.md](docs/04-testing.md) | Test strategy and CI gates |
| [05-deployment.md](docs/05-deployment.md) | Config, Docker, env vars, Ollama, MCP |
| [user-guide.md](docs/user-guide.md) | **User guide** — using the chat UI |
| [AGENTS.md](AGENTS.md) | AI agent index (skills, memory bank) |
| [ai-context-strategy.md](docs/ai-context-strategy.md) | Agent context architecture |

Reading order: Requirements → Architecture → Design → Testing → Deployment → Implement.

---

## License

See repository license file when added.
