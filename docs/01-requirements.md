# Requirements

## AI Chat (`ai-chat`) — SRS

**Version:** 1.0.0
**Date:** 2026-06-17
**Author:** Siarhei Berdachuk
**Status:** Draft
**Reference pattern:** [med-expert-match-ce](https://github.com/berdachuk/med-expert-match-ce)
**MCP backend:** [medical-mcp-server](../ai-architect-6-mcp)

### Related documents

| # | Document | Purpose |
|---|---|---|
| | [README.md](README.md) | Documentation index and development pipeline |
| 1 | [02-architecture.md](02-architecture.md) | System diagram, Modulith modules, stack, design decisions |
| 2 | [03-design.md](03-design.md) | Schema, services, MCP client, harness, chat classes |
| 3 | [04-testing.md](04-testing.md) | Test pyramid, quality gates |
| 4 | [05-deployment.md](05-deployment.md) | Config, Docker, env vars |

---

## 1. Overview

`ai-chat` is a **Spring AI 2.0.0 chat application** with a Thymeleaf SSR web UI that provides an AI-powered conversational interface. It connects to one or more **MCP servers** (Model Context Protocol) for context enrichment, supports long-running multi-turn dialogues with session memory, and displays agent progress via a Harness workflow engine.

The application mirrors the chat, session, harness, and LLM connection patterns from [`med-expert-match-ce`](https://github.com/berdachuk/med-expert-match-ce), stripped of graph databases (Apache AGE), medical domain entities, and evaluation frameworks — keeping only what is necessary for a general-purpose AI chat with MCP enrichment.

**Runtime identity**

| Property | Value |
|---|---|
| Port | `8080` |
| Transport | HTTP + SSE (Server-Sent Events) for streaming |
| Spring Boot | 4.1.0 |
| Spring AI | 2.0.0 |
| Spring Modulith | 2.1.0 |
| JDK | 21 |
| DB | PostgreSQL 17 (chat sessions + Spring AI Session JDBC) |
| LLM Provider | Ollama (OpenAI-compatible API) |
| Primary Model | `gemma4:31b-cloud` |
| Alternative Model | `gemma4:12b` |

---

## 2. Goals & Non-Goals

### Goals

- **Multi-session chat** — create, list, rename, delete chat sessions; each session has its own message history
- **Streaming responses** — SSE-based token-by-token streaming to the browser
- **Long dialog support** — Spring AI Session JDBC with turn-safe compaction (max 20 turns / 4000 tokens window)
- **MCP client integration** — connect to one or more MCP servers over SSE transport; auto-discover available tools
- **Automatic MCP tool selection** — the LLM decides which MCP tools to call based on conversation context
- **Harness workflow engine** — structured agent execution with planning, tool execution, verification, and policy gates
- **Agent progress display** — real-time SSE events showing agent activity (tool calls, reasoning, plan updates)
- **Structured Output** — LLM responses with typed JSON output where applicable
- **Multi-role LLM architecture** — separate models for chat (clinical-tier) and tool-calling (utility-tier)
- **Session memory** — conversation context mixed into each request via `SessionMemoryAdvisor`
- **Thymeleaf SSR frontend** — vanilla JS + Bootstrap 5.3, no SPA framework
- **Spring Modulith** package modules with `allowedDependencies` (same pattern as `med-expert-match-ce`)
- **Interface / implementation separation** — public contracts in `service/` and `repository/`; JDBC code only in `impl/` subpackages
- **JDBC only** — `NamedParameterJdbcTemplate`, no JPA/Hibernate

### Non-Goals

- Does not include medical domain entities (Doctor, MedicalCase, ICD-10, etc.)
- Does not include Apache AGE graph database
- Does not include GraphRAG, doctor matching, or facility routing
- Does not include document ingestion, chunking, or vector search
- Does not include evaluation framework
- Does not include FHIR adapters or synthetic data generation
- Does not include PubMed evidence retrieval
- Does not include API key authentication (local/dev default)
- Does not expose its own MCP server — it is an MCP **client** only

---

## 3. Chat Sessions

### Session model

Each chat session is an independent conversation thread with its own message history, agent profile, and session memory.

| Property | Description |
|---|---|
| `id` | 24-char hex string (MongoDB ObjectId format, same as `med-expert-match-ce`) |
| `userId` | User identifier from `X-User-Id` header or cookie |
| `name` | Auto-generated from first message (first 60 chars) or user-provided |
| `agentId` | Agent profile: `auto` (default) |
| `isDefault` | One default chat per user — auto-created on first visit |
| `messageCount` | Denormalized counter for list display |
| `createdAt`, `updatedAt`, `lastActivityAt` | Timestamps |

### Session operations

| Operation | Endpoint | Description |
|---|---|---|
| List chats | `GET /api/v1/chats` | All chats for current user, ordered by `lastActivityAt DESC` |
| Create chat | `POST /api/v1/chats` | New empty chat with optional name and agentId |
| Get history | `GET /api/v1/chats/{chatId}/history` | Paginated messages (limit, offset) |
| Delete chat | `DELETE /api/v1/chats/{chatId}` | Soft-delete messages, remove chat; recreate default if none left |
| Send message | `POST /api/v1/chats/{chatId}/messages/stream` | SSE streaming response |
| Rename chat | `PUT /api/v1/chats/{chatId}/name` | Update chat name |

### Message model

| Property | Description |
|---|---|
| `id` | 24-char hex string |
| `chatId` | FK to chat |
| `role` | `user` \| `assistant` \| `system` |
| `content` | Markdown-formatted text |
| `sequenceNumber` | Monotonic per chat (1, 2, 3, …) |
| `tokensUsed` | Estimated token count |
| `createdAt` | Timestamp |
| `metadata` | JSONB for tool calls, MCP context, agent activity |
| `deletedAt` | Soft-delete timestamp |

---

## 4. Long Dialog Support

### Session memory (Spring AI Session JDBC)

Session memory uses the same pattern as `med-expert-match-ce`:

| Property | Value |
|---|---|
| Session ID format | `{userId}-{chatId}` |
| Compaction strategy | `TurnWindowCompactionStrategy` (non-LLM) |
| Compaction trigger | 20 turns OR 4000 estimated tokens |
| Window size | 30 turns kept after compaction |
| Token estimator | `JTokkitTokenCountEstimator` (no external API call) |
| Advisor | `SessionMemoryAdvisor` with turn-safe semantics |

### Advisor chain (per request)

1. `DateTimeContextAdvisor` — injects current date/time
2. `MCPToolAdvisor` — provides MCP tool definitions to the LLM
3. `ToolCallingAdvisor` — enables tool calling with conversation history disabled
4. `SessionMemoryAdvisor` — injects compacted conversation history
5. `SimpleLoggerAdvisor` — request/response logging

---

## 5. MCP Client Integration

### MCP connection model

The application is an **MCP client** that connects to one or more MCP servers over SSE transport. It uses Spring AI's `McpSyncClient` to discover tools, resources, and prompts from each server.

| Property | Description |
|---|---|
| Transport | SSE (Server-Sent Events) |
| Client type | `McpSyncClient` (SYNC) |
| Connection config | `spring.ai.mcp.client.sse.connections.{name}.url` |
| Auto-discovery | On startup, each connection is initialized; tools are registered |
| Dynamic tool selection | LLM decides which MCP tools to invoke based on user query |

### MCP server connections (configurable)

```yaml
spring:
  ai:
    mcp:
      client:
        sse:
          connections:
            medical-dataset:
              url: ${MCP_MEDICAL_URL:http://localhost:8092/sse}
              tools: true
              resources: true
              prompts: true
            # Additional MCP servers can be added:
            # weather-service:
            #   url: http://localhost:8093/sse
```

### MCP tool discovery flow

1. On startup, `McpSyncClient` initializes SSE connection to each configured MCP server
2. Client calls `initialize()` → receives server capabilities (tools, resources, prompts)
3. Client calls `listTools()` → receives tool definitions (name, description, input schema)
4. Tools are wrapped as Spring AI `ToolCallback` instances and registered in the application context
5. `MCPToolAdvisor` provides available tool definitions to the LLM on each request
6. LLM decides which tools to call; `ToolCallingAdvisor` executes them via `McpSyncClient`

### Required MCP server: `medical-mcp-server`

After core chat functionality is implemented, the application **must** connect to the `medical-mcp-server` at `/home/berdachuk/projects-ai-architect/ai-architect-6-mcp` (port `8092`).

**Available tools from `medical-mcp-server`:**

| Tool | Description |
|---|---|
| `search_cases` | Full-text search over 2,464 medical cases |
| `get_case` | Retrieve full case transcription by UUID |
| `semantic_search` | Vector similarity search (nomic-embed-text:v1.5 @ 768 dims) |
| `list_specialties` | List 13 medical specialties with counts |
| `get_dataset_stats` | Dataset statistics |

**Available resources:** `medical://cases/{id}`, `medical://stats`
**Available prompts:** `case-analysis`

---

## 6. Harness Workflow Engine

### Overview

The Harness is a structured workflow engine (ported from `med-expert-match-ce`) that wraps LLM agent operations with planning, tool execution, verification, and policy gates. It provides real-time progress visibility to the user via SSE events.

### Harness workflow states

```
TASK_CREATED → PLANNING → CONTEXT_BUILT → TOOLS_EXECUTED → VERIFYING → POLICY_GATE → DONE
                                                                                    ↘ NEEDS_HUMAN
                                                                                    ↘ FAILED
```

### Harness components

| Component | Role |
|---|---|
| `ChatWorkflowEngine` | Orchestrates chat workflows: planning → context building → tool execution → verification → response |
| `AgentPlannerService` | Builds execution plans (steps + acceptance criteria) |
| `AgentResponseVerifier` | Verifies tool outputs against expected criteria |
| `PolicyGateService` | Reviews LLM responses for safety/quality |
| `HarnessWorkflowRunStore` | Persists workflow run state (JDBC) |
| `HarnessChainTraceStore` | Traces chained workflow events |

### Harness configuration

```yaml
ai-chat:
  harness:
    max-iterations: 2
    retry-on-verify-fail: true
    policy-gate-enabled: true
    human-checkpoint-enabled: false
```

### Agent progress display (SSE events)

The frontend receives SSE events and renders an **Agent Progress Panel** per turn:

| SSE Event | Frontend Display |
|---|---|
| `agent/agent_start` | Agent name + start timestamp |
| `activity/tool_call` | Tool name + arguments (collapsible) |
| `activity/reasoning` | Reasoning text (collapsible `<details>`) |
| `activity/todo_update` | Plan step status update |
| `activity/llm_call` | LLM invocation with token count |
| `pipeline_stage` | Workflow stage transition |
| `agent/agent_done` | Agent completion with summary (N steps, S seconds) |
| `token` | Streaming response text |
| `done` | Full response complete |

---

## 7. LLM Connections

### Multi-role architecture

Same pattern as `med-expert-match-ce` — separate models for different roles:

| Role | Default Model | Purpose | Config Prefix |
|---|---|---|---|
| **chat** (primary) | `gemma4:31b-cloud` | Main conversation, reasoning, structured output | `CHAT_*` |
| **chat** (alternative) | `gemma4:12b` | Fallback for lighter queries | `CHAT_ALT_*` |
| **tool-calling** | `functiongemma:270m` | Function calling, tool invocation | `TOOL_CALLING_*` |

### Configuration

```yaml
spring:
  ai:
    custom:
      chat:
        provider: openai
        base-url: ${CHAT_BASE_URL:http://localhost:11434/v1}
        api-key: ${CHAT_API_KEY:none}
        model: ${CHAT_MODEL:gemma4:31b-cloud}
        temperature: 0.7
        max-tokens: 6000
      chat-alt:
        provider: openai
        base-url: ${CHAT_ALT_BASE_URL:http://localhost:11434/v1}
        api-key: ${CHAT_ALT_API_KEY:none}
        model: ${CHAT_ALT_MODEL:gemma4:12b}
        temperature: 0.7
        max-tokens: 6000
      tool-calling:
        provider: openai
        base-url: ${TOOL_CALLING_BASE_URL:http://localhost:11434/v1}
        api-key: ${TOOL_CALLING_API_KEY:none}
        model: ${TOOL_CALLING_MODEL:functiongemma:270m}
        temperature: 0.1
        max-tokens: 2048
```

### ChatModel creation

- `OpenAiChatModelFactory.create()` builds `OpenAiChatModel` from resolved endpoint properties
- All ChatModels are `@Lazy` to avoid initialization overhead
- Auto-config disabled for: `OpenAiChatAutoConfiguration`, `OpenAiEmbeddingAutoConfiguration`, `OpenAiAudioSpeechAutoConfiguration`, `OpenAiAudioTranscriptionAutoConfiguration`, `OpenAiImageAutoConfiguration`
- Provider: **OpenAI-compatible API** (Ollama uses `/v1` suffix)

### Structured Output

The chat model supports **Structured Output** via Spring AI's `@Tool` annotations and typed response records. When the LLM needs to produce structured data (e.g., MCP tool call arguments, plan steps), it uses JSON schema-constrained generation.

---

## 8. Frontend (Thymeleaf SSR)

### Technology

| Component | Choice |
|---|---|
| Server rendering | Thymeleaf 3.x |
| CSS framework | Bootstrap 5.3 (CDN) |
| Markdown rendering | `marked.js` + `DOMPurify` (CDN) |
| Streaming | Fetch API + SSE body parsing |
| JavaScript | Vanilla JS (no framework) |

### Chat UI layout

```
┌──────────────────────────────────────────────────────────┐
│  Header: AI Chat                              [New Chat] │
├────────────┬─────────────────────────────────────────────┤
│  Sidebar   │  Message Panel                              │
│            │  ┌──────────────────────────────────────┐   │
│  Chat 1    │  │ User: What are cardiovascular...     │   │
│  Chat 2    │  │ Assistant: Cardiovascular diseases... │   │
│  Chat 3    │  │ [Agent Panel: 1 agent · 3 steps]    │   │
│  ...       │  └──────────────────────────────────────┘   │
│            │  Composer: [textarea] [Send]               │
│  [Delete   │                                             │
│   All]     │                                             │
└────────────┴─────────────────────────────────────────────┘
```

### SSE event handling (chat.js)

Same pattern as `med-expert-match-ce` `chat.js` (769 lines):

- **`token`** events → append to streaming message, render Markdown
- **`agent`** events → create/update agent progress panel
- **`activity`** events → add entries to agent panel (tool_call, reasoning, todo_update)
- **`pipeline_stage`** events → update workflow stage indicator
- **`done`** event → finalize message, collapse agent panel with summary
- **Reasoning split** — detects "strategizing complete" markers, wraps reasoning in `<details>` collapsible

---

## 9. Database Schema

### Tables

Single Flyway migration `V1__init_chat_schema.sql`:

```sql
CREATE SCHEMA IF NOT EXISTS ai_chat;

-- Chat sessions
CREATE TABLE ai_chat.chat (
    id               CHAR(24)    PRIMARY KEY,
    user_id          VARCHAR(255) NOT NULL,
    name             VARCHAR(255),
    agent_id         VARCHAR(50)  DEFAULT 'auto',
    is_default       BOOLEAN      DEFAULT FALSE,
    created_at       TIMESTAMPTZ  DEFAULT now(),
    updated_at       TIMESTAMPTZ  DEFAULT now(),
    last_activity_at TIMESTAMPTZ  DEFAULT now(),
    message_count    INT          DEFAULT 0
);
CREATE UNIQUE INDEX idx_chat_user_default ON ai_chat.chat (user_id, is_default) WHERE is_default = TRUE;

-- Chat messages
CREATE TABLE ai_chat.chat_message (
    id              CHAR(24)    PRIMARY KEY,
    chat_id         CHAR(24)    NOT NULL REFERENCES ai_chat.chat(id) ON DELETE CASCADE,
    role            VARCHAR(20)  NOT NULL CHECK (role IN ('user', 'assistant', 'system')),
    content         TEXT         NOT NULL,
    sequence_number INT          NOT NULL,
    tokens_used     INT,
    created_at      TIMESTAMPTZ  DEFAULT now(),
    metadata        JSONB,
    deleted_at      TIMESTAMPTZ
);
CREATE UNIQUE INDEX idx_chat_message_seq ON ai_chat.chat_message (chat_id, sequence_number);

-- Spring AI Session JDBC tables (managed by spring-ai-starter-session-jdbc)
-- ai_session, ai_session_event — auto-created by Spring AI
```

### ID generation

24-char hex strings via `IdGenerator.generateId()` — same MongoDB ObjectId-compatible format as `med-expert-match-ce`.

---

## 10. Architecture (Spring Modulith)

**Canonical reference:** [02-architecture.md](02-architecture.md)

Single deployable Spring Boot application using **package-based modules**. Each module has a `package-info.java` annotated with `@ApplicationModule(allowedDependencies = …)`.

### Package modules

```
src/main/java/com/example/aichat/
├── AiChatApplication.java
├── core/              # Shared config, security, health, util
├── chat/              # Chat domain, repository, service, REST controller
├── llm/               # LLM orchestration, harness, tools, advisors
├── mcp/               # MCP client config, tool discovery, tool wrappers
├── web/               # Thymeleaf SSR web controllers
└── system/            # Actuator health indicators
```

### Module dependency graph

```
web     → core, chat, llm
chat    → core
llm     → core, chat, mcp
mcp     → core
system  → core
```

### Interface / implementation rules

| Layer | Public API | Implementation |
|---|---|---|
| Repository | `{module}/repository/XxxRepository.java` | `{module}/repository/impl/XxxRepositoryImpl.java` |
| Service | `{module}/service/XxxService.java` | `{module}/service/impl/XxxServiceImpl.java` |
| MCP adapters | `mcp/*` — delegates to service interfaces | No JDBC in MCP layer |

---

## 11. Key Dependencies

| Dependency | Version | Notes |
|---|---|---|
| Spring Boot | 4.1.0 | Parent POM |
| Spring AI BOM | 2.0.0 | |
| Spring Modulith BOM | 2.1.0 | |
| Java | 21 | |
| `spring-boot-starter-web` | (Boot BOM) | Thymeleaf SSR + REST |
| `spring-boot-starter-thymeleaf` | (Boot BOM) | Server-side rendering |
| `spring-boot-starter-jdbc` | (Boot BOM) | NamedParameterJdbcTemplate |
| `spring-boot-starter-flyway` | (Boot BOM) | Schema migrations |
| `flyway-database-postgresql` | (Boot BOM) | |
| `postgresql` | 42.7.11 | |
| `spring-modulith-core` | 2.1.0 | `@ApplicationModule` |
| `spring-ai-starter-model-openai` | 2.0.0 | ChatModel + ToolCalling |
| `spring-ai-starter-session-jdbc` | 0.3.0 | Session memory (community) |
| `spring-ai-agent-utils` | 0.10.0 | Agent tools (TodoWrite, AskUserQuestion) |
| `spring-ai-starter-mcp-client` | 2.0.0 | MCP client (SSE transport) |
| `caffeine` | 3.2.4 | Local cache |
| `spring-boot-starter-actuator` | (Boot BOM) | Health, metrics |
| `spring-boot-starter-security` | (Boot BOM) | Permit-all for local/dev |
| `spring-boot-starter-validation` | (Boot BOM) | |
| `jackson` | (Boot BOM) | JSON serialization |
| Testcontainers | 2.0.5 | Integration tests |
| `spring-modulith-starter-test` | 2.1.0 | Modulith verification |

---

## 12. Configuration

```yaml
spring:
  application:
    name: ai-chat
  autoconfigure:
    exclude:
      - org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration
      - org.springframework.ai.model.openai.autoconfigure.OpenAiEmbeddingAutoConfiguration
      - org.springframework.ai.model.openai.autoconfigure.OpenAiAudioSpeechAutoConfiguration
      - org.springframework.ai.model.openai.autoconfigure.OpenAiAudioTranscriptionAutoConfiguration
      - org.springframework.ai.model.openai.autoconfigure.OpenAiImageAutoConfiguration
  datasource:
    url: jdbc:postgresql://${AICHAT_DB_HOST:localhost}:5432/${AICHAT_DB_NAME:ai_chat}
    username: ${AICHAT_DB_USERNAME:ai_chat}
    password: ${AICHAT_DB_PASSWORD:ai_chat}
    hikari:
      maximum-pool-size: 10
      minimum-idle: 3
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
    default-schema: ai_chat
  ai:
    openai:
      enabled: false
    custom:
      chat:
        provider: openai
        base-url: ${CHAT_BASE_URL:http://localhost:11434/v1}
        api-key: ${CHAT_API_KEY:none}
        model: ${CHAT_MODEL:gemma4:31b-cloud}
        temperature: 0.7
        max-tokens: 6000
      chat-alt:
        provider: openai
        base-url: ${CHAT_ALT_BASE_URL:http://localhost:11434/v1}
        api-key: ${CHAT_ALT_API_KEY:none}
        model: ${CHAT_ALT_MODEL:gemma4:12b}
        temperature: 0.7
        max-tokens: 6000
      tool-calling:
        provider: openai
        base-url: ${TOOL_CALLING_BASE_URL:http://localhost:11434/v1}
        api-key: ${TOOL_CALLING_API_KEY:none}
        model: ${TOOL_CALLING_MODEL:functiongemma:270m}
        temperature: 0.1
        max-tokens: 2048
    mcp:
      client:
        sse:
          connections:
            medical-dataset:
              url: ${MCP_MEDICAL_URL:http://localhost:8092/sse}
              tools: true
              resources: true
              prompts: true
    session:
      jdbc:
        schema: ai_chat
        compaction:
          strategy: turn-window
          max-turns: 20
          max-tokens: 4000
          window-turns: 30

server:
  port: ${SERVER_PORT:8080}
  shutdown: graceful

management:
  endpoints:
    web:
      exposure:
        include: health, info, metrics
  endpoint:
    health:
      show-details: always
      probes:
        enabled: true

ai-chat:
  harness:
    max-iterations: 2
    retry-on-verify-fail: true
    policy-gate-enabled: true
    human-checkpoint-enabled: false
  features:
    mcp-client: true
    harness: true
    structured-output: true
```

---

## 13. Environment Variables

| Variable | Default | Description |
|---|---|---|
| `AICHAT_DB_HOST` | `localhost` | PostgreSQL host |
| `AICHAT_DB_NAME` | `ai_chat` | Database name |
| `AICHAT_DB_USERNAME` | `ai_chat` | DB user |
| `AICHAT_DB_PASSWORD` | `ai_chat` | DB password |
| `CHAT_BASE_URL` | `http://localhost:11434/v1` | Primary chat model endpoint |
| `CHAT_API_KEY` | `none` | API key for chat model |
| `CHAT_MODEL` | `gemma4:31b-cloud` | Primary chat model name |
| `CHAT_ALT_BASE_URL` | `http://localhost:11434/v1` | Alternative chat model endpoint |
| `CHAT_ALT_API_KEY` | `none` | API key for alt model |
| `CHAT_ALT_MODEL` | `gemma4:12b` | Alternative chat model name |
| `TOOL_CALLING_BASE_URL` | `http://localhost:11434/v1` | Tool-calling model endpoint |
| `TOOL_CALLING_API_KEY` | `none` | API key for tool-calling model |
| `TOOL_CALLING_MODEL` | `functiongemma:270m` | Tool-calling model name |
| `MCP_MEDICAL_URL` | `http://localhost:8092/sse` | Medical MCP server SSE endpoint |
| `SERVER_PORT` | `8080` | Application port |

---

## 14. Milestones

| # | Milestone | Key deliverables | Status |
|---|---|---|---|
| M1 | Schema + modulith foundation | `V1__init_chat_schema.sql`, domain records, `package-info.java` per module, Boot stub | ⬜ |
| M2 | Chat session CRUD | `ChatRepository` + impl, `ChatService` + impl, `ChatController` REST endpoints | ⬜ |
| M3 | LLM integration | `SpringAIConfig`, `OpenAiChatModelFactory`, `ChatAssistantService` + impl, streaming SSE | ⬜ |
| M4 | Session memory | Spring AI Session JDBC, `SessionMemoryAdvisor`, compaction, `DateTimeContextAdvisor` | ⬜ |
| M5 | Harness engine | `ChatWorkflowEngine`, `AgentPlannerService`, `AgentResponseVerifier`, `PolicyGateService` | ⬜ |
| M6 | Frontend (Thymeleaf SSR) | `chat.html`, `chat.js`, agent progress panel, sidebar, composer | ⬜ |
| M7 | MCP client integration | `McpSyncClient` config, tool discovery, `MCPToolAdvisor`, tool wrappers | ⬜ |
| M8 | Medical MCP server connection | Connect to `medical-mcp-server` (:8092), test all 5 tools in chat flow | ⬜ |
| M9 | Docker + polish | `docker-compose.yml`, `Dockerfile`, full integration test | ⬜ |

---

## 15. Docker Compose

```yaml
services:
  ai-chat:
    build: .
    ports:
      - "8080:8080"
    environment:
      AICHAT_DB_HOST: postgres
      AICHAT_DB_USERNAME: ai_chat
      AICHAT_DB_PASSWORD: ai_chat
      CHAT_BASE_URL: http://host.docker.internal:11434/v1
      CHAT_API_KEY: none
      CHAT_MODEL: gemma4:31b-cloud
      TOOL_CALLING_BASE_URL: http://host.docker.internal:11434/v1
      TOOL_CALLING_API_KEY: none
      TOOL_CALLING_MODEL: functiongemma:270m
      MCP_MEDICAL_URL: http://host.docker.internal:8092/sse
    depends_on:
      postgres:
        condition: service_healthy
    extra_hosts:
      - host.docker.internal:host-gateway
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3

  postgres:
    image: postgres:17
    environment:
      POSTGRES_DB: ai_chat
      POSTGRES_USER: ai_chat
      POSTGRES_PASSWORD: ai_chat
    volumes:
      - pgdata:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ai_chat"]
      interval: 10s
      timeout: 5s
      retries: 5

volumes:
  pgdata:
```

Ollama runs on the host (`host.docker.internal:11434`), not in Docker.

---

## Related documentation

- [README.md](README.md) — documentation index
- [02-architecture.md](02-architecture.md) — system design, Modulith layout, stack
- [03-design.md](03-design.md) — detailed design and class sketches
- [04-testing.md](04-testing.md) — test strategy
- [05-deployment.md](05-deployment.md) — config, Docker, env vars
