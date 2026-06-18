# ai-architect-6-chat

General-purpose AI chat with multi-session history, long-dialog memory, optional MCP context enrichment, and Harness-style agent progress.

**Core chat works without MCP** — sessions, streaming, and session memory do not depend on any MCP server. MCP adds tool enrichment when a server is available.

Built on patterns from [med-expert-match-ce](https://github.com/berdachuk/med-expert-match-ce). Optional MCP enrichment via [ai-architect-6-mcp](https://github.com/berdachuk/ai-architect-6-mcp).

**Version:** 1.0.0 · **Status:** `develop` — milestones M1–M9 complete; Playwright E2E, optional OAuth2, Prometheus/Grafana

---

## Overview

`ai-chat` is a **Spring Boot 4.1** + **Spring AI 2.0** application with a Thymeleaf SSR web UI. Users manage multiple chat sessions, stream assistant responses token-by-token over SSE, and see real-time agent activity (tool calls, planning steps, pipeline stages) in a collapsible progress panel.

| Property                 | Value                                                            |
|--------------------------|------------------------------------------------------------------|
| Port                     | `8095`                                                           |
| Transport                | HTTP + SSE (`text/event-stream`)                                 |
| Database                 | PostgreSQL 17 (`ai_chat` schema)                                 |
| LLM provider             | Spring AI `ollama` (`spring.ai.custom.*`)                        |
| LLM backend (default)    | Ollama `http://localhost:11434`                                  |
| Base package             | `com.berdachuk.aichat`                                           |
| MCP transport            | SSE (`McpSyncClient`); runtime catalog via REST                  |
| Identity (default)       | `X-User-Id` header / `aichat-user-id` cookie — no login required |
| Health                   | `GET /actuator/health`                                           |
| Metrics (`prod` profile) | `GET /actuator/prometheus`                                       |

---

## Features

| Feature                 | Description                                                                                        |
|-------------------------|----------------------------------------------------------------------------------------------------|
| **Multi-session chat**  | Create, list, rename, delete sessions; one default chat per user                                   |
| **Streaming responses** | SSE token streaming with Markdown rendering (`marked.js` + DOMPurify)                              |
| **Long dialog support** | Spring AI Session JDBC with turn-window compaction (20 turns / 4000 tokens)                        |
| **Harness workflow**    | `ChatWorkflowEngine` — planning, tool execution, verification, policy gate                         |
| **Agent progress UI**   | SSE events: `activity`, `pipeline_stage`, `agent`, `tool_call`                                     |
| **MCP client**          | Runtime catalog (`POST /api/v1/mcp/connections`); per-chat toggles; graceful degradation when down |
| **Multi-role LLM**      | `AICHAT_CHAT_MODEL`, `AICHAT_TOOL_MODEL` (default `gemma4:31b-cloud`)            |
| **Security**            | Open by default (`ai-chat.security.oauth2-enabled: false`); optional JWT via `oauth2` profile      |
| **Observability**       | Actuator health (MCP indicator in test profile); Prometheus + Grafana dashboard in `prod`          |
| **CI**                  | `mvn test`, `mvn verify -Pintegration`, Playwright E2E against Docker Compose                      |
| **Spring Modulith**     | Package modules with `allowedDependencies`; `verify()` in CI                                       |

---

## Architecture

Single deployable Spring Boot app with package-based Modulith modules:

```text
src/main/java/com/berdachuk/aichat/
├── core/     # Config, security, OpenAiChatModelFactory, UserContext
├── chat/     # Sessions, messages, REST /api/v1/chats
├── llm/      # ChatAssistantService, Harness, advisors, SSE activity
├── mcp/      # McpServerRegistry, McpClientConnector, REST catalog
├── web/      # Thymeleaf SSR (chat.html, chat.js)
└── system/   # Actuator MCP health indicator
```

```text
Browser (Thymeleaf + chat.js)
        │  HTTP + SSE
        ▼
   ai-chat :8095
        ├── PostgreSQL 17  (chat, message, mcp_connection, ai_session)
        ├── Ollama         (default gemma4:31b-cloud — AICHAT_* env vars)
        └── MCP server(s)  (optional — e.g. ai-architect-6-mcp :8092/sse)
```

Details: [docs/02-architecture.md](docs/02-architecture.md)

---

## Technology stack

| Component                        | Version                                  |
|----------------------------------|------------------------------------------|
| Java                             | 21                                       |
| Spring Boot                      | 4.1.0                                    |
| Spring AI                        | 2.0.0 BOM                                |
| Spring Modulith                  | 2.1.0                                    |
| PostgreSQL                       | 17                                       |
| `spring-ai-starter-session-jdbc` | 0.3.0                                    |
| `spring-ai-starter-mcp-client`   | 2.0.0                                    |
| OAuth2 resource server           | optional (`application-oauth2.yml`)      |
| Micrometer Prometheus            | `prod` profile                           |
| Frontend                         | Thymeleaf 3 + Bootstrap 5.3 + vanilla JS |
| E2E                              | Playwright (`e2e/`)                      |

---

## LLM configuration

Defaults match `src/main/resources/application.yml`.

| Role                | Env var                 | Default                  | Purpose                       |
|---------------------|-------------------------|--------------------------|-------------------------------|
| All roles base URL  | `AICHAT_CHAT_BASE_URL`       | `http://localhost:11434` | Ollama host (no `/v1` suffix) |
| API key placeholder | `AICHAT_CHAT_API_KEY`        | `ollama`                 | Passed to Spring AI client    |
| Primary chat        | `AICHAT_CHAT_MODEL`     | `gemma4:31b-cloud`       | Streaming responses           |
| Tool calling        | `AICHAT_TOOL_MODEL`     | `functiongemma:270m`    | MCP tool invocation           |

```bash
ollama pull gemma4:31b-cloud
ollama pull functiongemma:270m
# optional: export AICHAT_CHAT_MODEL=gemma4:31b-cloud
```

---

## Quick start

**Prerequisites:** JDK 21, Maven 3.9+, Docker (WSL 2 on Windows), Ollama for live LLM replies.

### Docker Compose (recommended)

```bash
docker compose up --build
```

Open `http://localhost:8095/`

With [ai-architect-6-mcp](https://github.com/berdachuk/ai-architect-6-mcp) on the host (`:8092`):

```bash
docker compose -f docker-compose.yml -f docker-compose.mcp-host.yml up --build
```

### Local development

Start only PostgreSQL via Docker Compose (app runs on the host via Maven/IDE):

```bash
docker compose -f docker-compose.dev.yml up -d
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

The `dev` profile enables DEBUG logging, detailed `/actuator/health`, and Prometheus
export. PostgreSQL is exposed on `localhost:5437` (matches `application.yml` default);
data lives in the isolated `pgdata-dev` volume.

Alternative (raw `docker run`):

```bash
docker run -d --name ai-chat-postgres \
  -e POSTGRES_DB=ai_chat -e POSTGRES_USER=ai_chat -e POSTGRES_PASSWORD=ai_chat \
  -p 5437:5432 postgres:17

mvn spring-boot:run
```

### Tests

```bash
mvn test                      # unit + Modulith architecture
mvn verify -Pintegration        # + Testcontainers integration tests
bash scripts/smoke-rest.sh http://localhost:8095   # REST smoke (app running)

# Playwright browser E2E (app via Docker)
docker compose -f docker-compose.yml -f docker-compose.e2e.yml up -d --build --wait
cd e2e && npm ci && npx playwright install chromium && npm test
```

CI runs all of the above (Maven + Playwright) on push to `develop` / `main`.

---

## API overview

| Method        | Path                                 | Description                       |
|---------------|--------------------------------------|-----------------------------------|
| `GET`         | `/api/v1/chats`                      | List sessions for current user    |
| `POST`        | `/api/v1/chats`                      | Create session                    |
| `GET`         | `/api/v1/chats/{id}/history`         | Message history                   |
| `PUT`         | `/api/v1/chats/{id}/name`            | Rename session                    |
| `DELETE`      | `/api/v1/chats/{id}`                 | Delete session                    |
| `POST`        | `/api/v1/chats/{id}/messages/stream` | Send message — SSE response       |
| `GET` / `PUT` | `/api/v1/chats/{id}/mcp`             | Per-chat MCP connection selection |
| `GET`         | `/api/v1/mcp/connections`            | MCP catalog                       |
| `POST`        | `/api/v1/mcp/connections`            | Register MCP server               |
| `DELETE`      | `/api/v1/mcp/connections/{id}`       | Remove MCP server                 |

**Identity (default):** `X-User-Id` header or `aichat-user-id` cookie (fallback `anonymous`).

**Optional OAuth2:** `SPRING_PROFILES_ACTIVE=oauth2` — JWT required on `/api/v1/**`; see [docs/05-deployment.md](docs/05-deployment.md).

OpenAPI UI: `http://localhost:8095/swagger-ui.html`

---

## Observability

| Endpoint               | Profile  | Description                            |
|------------------------|----------|----------------------------------------|
| `/actuator/health`     | default  | Liveness; MCP status hidden by default |
| `/actuator/prometheus` | `prod`   | Prometheus scrape target               |

Grafana dashboard: [observability/grafana/ai-chat-overview.json](observability/grafana/ai-chat-overview.json)

```bash
SPRING_PROFILES_ACTIVE=prod mvn spring-boot:run
```

---

## Implementation status

| Phase           | Milestones                       | Status   |
|-----------------|----------------------------------|----------|
| Core chat       | M1–M6                            | Complete |
| MCP client + UI | M7–M8                            | Complete |
| Docker + CI     | M9                               | Complete |
| Post-release    | OAuth2, E2E, Grafana, user guide | Complete |

Requirements: [docs/01-requirements.md §14](docs/01-requirements.md#14-milestones)

---

## Reference repositories

| Repository                                                              | Role                                                      |
|-------------------------------------------------------------------------|-----------------------------------------------------------|
| [med-expert-match-ce](https://github.com/berdachuk/med-expert-match-ce) | Chat CRUD, SSE, session memory, Harness UI, JDBC patterns |
| [ai-architect-6-mcp](https://github.com/berdachuk/ai-architect-6-mcp)   | Optional MCP backend on `:8092/sse`                       |

**Ported:** `chat/` module, `ChatStreamActivityPublisher`, session compaction, `chat.js` agent panel.

**New:** `mcp/` client, `McpServerRegistry`, `MCPToolAdvisor`, `ChatWorkflowEngine`, runtime MCP catalog.

---

## Documentation

Full index: **[docs/README.md](docs/README.md)**

| Document| Purpose |
|---|---|
| [user-guide.md](docs/user-guide.md) | **End-user guide** — chat UI, sessions, MCP |
| [01-requirements.md](docs/01-requirements.md) | Software requirements (SRS) |
| [02-architecture.md](docs/02-architecture.md) | Software architecture (SAD) |
| [03-design.md](docs/03-design.md) | Detailed design (SDD) |
| [04-testing.md](docs/04-testing.md) | Test strategy, smoke checklist, Playwright |
| [05-deployment.md](docs/05-deployment.md) | Docker, env vars, OAuth2, Prometheus |
| [AGENTS.md](AGENTS.md) | AI agent index |

---

## License

See [LICENSE](LICENSE) — MIT.

Release history: [RELEASE.md](RELEASE.md)
