# Requirements

## AI Chat (`ai-chat`) — SRS

**Version:** 1.0.0
**Date:** 2026-06-17
**Author:** Siarhei Berdachuk
**Status:** Draft
**Reference pattern:** [med-expert-match-ce](https://github.com/berdachuk/med-expert-match-ce)
**MCP backend:** [ai-architect-6-mcp](https://github.com/berdachuk/ai-architect-6-mcp) (`medical-mcp-server`, port `8092`)

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

**MCP is optional at runtime.** The chat must remain fully functional — sessions, streaming, session memory, Harness progress — when no MCP server is configured, unreachable at startup, or fails mid-session. MCP only adds context enrichment when available; it is never a hard dependency for core chat.

**Runtime identity**

| Property | Value |
|---|---|
| Port | `8095` |
| Transport | HTTP + SSE (Server-Sent Events) for streaming |
| Spring Boot | **4.x** (baseline **4.1.0** — latest stable 4.x patch) |
| Spring AI | **2.0.0** (BOM; latest stable 2.0.x patch) |
| Spring Modulith | **2.1.0** (aligned with Boot 4.x) |
| JDK | 21 |
| DB | PostgreSQL 17 (chat sessions + Spring AI Session JDBC) |
| LLM client | Spring AI **OpenAI-compatible** client (`OpenAiChatModel` / `OpenAiApi`) |
| LLM backend (default) | **Ollama** at `http://localhost:11434/v1` |
| Primary Model | `gemma4:31b-cloud` |
| Alternative Model | `gemma4:12b` |
| Base package | `com.berdachuk.aichat` (organization namespace `com.berdachuk`; same convention as `com.berdachuk.medexpertmatch` in med-expert-match-ce) |

---

## 1.1 Reference analysis

### med-expert-match-ce — what to reuse

Analyzed at HEAD (2026-06-17). The chat subsystem lives in three packages:

| Package | Key artifacts | Reuse for ai-chat |
|---|---|---|
| `chat/` | `ChatController`, `ChatService`, `ChatRepository`, domain records, `sql/chat/*` | **Port directly** (rename schema to `ai_chat`) |
| `llm/` | `ChatAssistantServiceImpl`, `ChatStreamActivityPublisherImpl`, session beans in `MedicalAgentConfiguration` | **Port patterns** — strip medical routing |
| `web/` | `ChatWebController`, `chat.html`, `chat.js`, `chat.css` | **Port UI** — remove medical explainability panels |
| `core/` | `SpringAIConfig`, `OpenAiChatModelFactory`, `DateTimeContextAdvisor` | **Port directly** |

**Critical finding:** med-expert-match-ce has **no MCP client**. Tool calling uses Spring AI `@Tool` beans and `spring-ai-agent-utils` (`TodoWriteTool`, `AskUserQuestionTool`, `SkillsTool`). ai-chat adds MCP as a **new capability** via `spring-ai-starter-mcp-client`.

**Harness in reference:** medical-specific engines (`DoctorMatchWorkflowEngine`, `RoutingWorkflowEngine`, `CaseIntakeWorkflowEngine`). ai-chat introduces a **generic** `ChatWorkflowEngine` with the same state machine and SSE progress pattern, without medical domain steps.

**Advisor chain (reference `medicalAgentChatClient`):**

1. `DateTimeContextAdvisor`
2. `ToolCallingAdvisor` (`conversationHistoryEnabled: false`)
3. `SessionMemoryAdvisor` (JDBC + turn-window compaction)
4. `SimpleLoggerAdvisor`

ai-chat extends this with `MCPToolAdvisor` (position 2) when MCP is enabled.

**SSE contract (reference `ChatAssistantServiceImpl` + `chat.js`):**

| Event | Payload |
|---|---|
| `token` | `{"t":"<chunk>"}` |
| `done` | `{"id":"<msgId>","content":"<full text>", ...}` |
| `agent` | `{"type":"agent_start"\|"agent_done","agentId":"..."}` |
| `activity` | `{"type":"tool_call"\|"reasoning"\|"todo_update"\|"llm_call"\|"llm_turn_summary", ...}` |
| `pipeline_stage` | `{"stage":"PLANNING","agent":"...","status":"...","timestampMs":N}` |

Activity events are bridged from Spring application events via `ChatStreamActivityPublisherImpl` (registers `SseEmitter` per `sessionId = userId-chatId`).

### ai-architect-6-mcp — integration contract

| Property | Value |
|---|---|
| SSE URL | `http://localhost:8092/sse` |
| Message endpoint | `POST /mcp/message` |
| Protocol | MCP SYNC over HTTP+SSE |
| Tools | `search_cases`, `get_case`, `semantic_search`, `list_specialties`, `get_dataset_stats` |
| Resources | `medical://cases/{id}`, `medical://stats` |
| Prompts | `case-analysis` (template only — LLM runs on client side) |

Connection is configured in phase 2 (milestone M8). When the server is unreachable, the chat must degrade gracefully — tools omitted, health indicator reports DOWN, **streaming chat continues**.

### LLM model divergence from reference

med-expert-match-ce routes the **entire** `medicalAgentChatClient` through `toolCallingChatModel` (functiongemma) because its primary clinical model lacks reliable tool calling.

ai-chat uses a **split architecture**:

| Role | Model | Used for |
|---|---|---|
| Chat (primary) | `gemma4:31b-cloud` | Reasoning, streaming response, structured output |
| Chat (alt) | `gemma4:12b` | Lighter fallback when configured or on primary failure |
| Tool calling | `functiongemma:270m` | MCP tool invocation only (`ToolCallingAdvisor`) |

---

## 2. Goals & Non-Goals

### Goals

- **Multi-session chat** — create, list, rename, delete chat sessions; each session has its own message history
- **Streaming responses** — SSE-based token-by-token streaming to the browser
- **Long dialog support** — Spring AI Session JDBC with turn-safe compaction (max 20 turns / 4000 tokens window)
- **MCP client integration** — connect to one or more MCP servers over SSE transport when available; auto-discover tools
- **Runtime MCP catalog** — add, edit, or remove MCP connection definitions through the **chat UI or REST API** without recompiling or redeploying application code; definitions persisted (not Java-only config)
- **Per-chat MCP context** — during a chat session, the user selects which MCP connections from the **configurable catalog** are active for that chat; only selected, reachable servers contribute tools to the turn
- **Chat without MCP** — core chat (CRUD, SSE streaming, session memory, Harness UI) works with zero MCP servers; startup must not fail if MCP is down
- **Automatic MCP server awareness** — on startup, discover tools/resources/prompts from each configured MCP connection; expose catalog to LLM per turn
- **Automatic MCP tool selection** — the LLM decides which MCP tools to call based on conversation context (via `ToolCallingAdvisor` + `functiongemma`)
- **Harness workflow engine** — structured agent execution with planning, tool execution, verification, and policy gates
- **Agent progress display** — real-time SSE events showing agent activity (tool calls, reasoning, plan updates)
- **Structured Output** — LLM responses with typed JSON output where applicable
- **Multi-role LLM architecture** — separate models for chat (`gemma4`) and tool-calling (`functiongemma`)
- **OpenAI-compatible LLM client** — all chat and tool-calling traffic via Spring AI `OpenAiChatModel`; **default backend Ollama** (`/v1` API); endpoints overridable per role via env vars
- **Session memory** — conversation context mixed into each request via `SessionMemoryAdvisor`
- **Thymeleaf SSR frontend** — vanilla JS + Bootstrap 5.3, no SPA framework
- **Spring Modulith** package modules with `allowedDependencies` (same pattern as `med-expert-match-ce`)
- **Base package `com.berdachuk.aichat`** — all application code under `com.berdachuk.aichat.{core,chat,llm,mcp,web,system}`; do not use `com.example` or other placeholder namespaces
- **Interface / implementation separation** — public contracts in `service/` and `repository/`; JDBC code only in `impl/` subpackages
- **JDBC only** — `NamedParameterJdbcTemplate`, no JPA/Hibernate
- **Externalized SQL** — no inline SQL in Java; `@InjectSql` + files under `sql/<module>/`; named bind parameters (`:name`) only (DEC-013)
- **Latest stable dependencies** — use current stable releases; **Spring Boot 4.x** and **Spring AI 2.0.0** BOM are mandatory baselines (see §11.1)

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
- Does not target Spring Boot 3.x or Spring AI 1.x — **Boot 4.x + Spring AI 2.0.0** only

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
| `enabledMcpConnections` | List of MCP connection IDs active for this chat (subset of catalog; default empty or user-chosen) |
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
| List MCP catalog | `GET /api/v1/mcp/connections` | All configured MCP connections (id, name, url, status, tool count) |
| Add MCP connection | `POST /api/v1/mcp/connections` | Register new connection at runtime — **no code change** |
| Remove MCP connection | `DELETE /api/v1/mcp/connections/{connectionId}` | Remove from catalog; detach from chats |
| Get chat MCP selection | `GET /api/v1/chats/{chatId}/mcp` | MCP connection IDs enabled for this chat |
| Set chat MCP selection | `PUT /api/v1/chats/{chatId}/mcp` | User picks which catalog entries apply to this session's context |

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

When MCP is enabled (`ai-chat.features.mcp-client: true`):

1. `DateTimeContextAdvisor` — injects current date/time
2. `MCPToolAdvisor` — injects MCP tool catalog + registers `ToolCallback` wrappers (skipped if no servers reachable)
3. `ToolCallingAdvisor` — executes tool calls via `functiongemma` (`conversationHistoryEnabled: false`)
4. `SessionMemoryAdvisor` — injects compacted conversation history for `sessionId = {userId}-{chatId}`
5. `SimpleLoggerAdvisor` — request/response logging

Per-turn context params (same as reference `SessionAdvisorSupport`):

- `SessionMemoryAdvisor.SESSION_ID_CONTEXT_KEY` → `{userId}-{chatId}`
- `SessionMemoryAdvisor.EVENT_FILTER_CONTEXT_KEY` → orchestrator branch filter

When MCP is disabled or no servers are reachable, step 2 is skipped (chain collapses to 4 advisors).

---

## 5. MCP Client Integration

### Dynamic MCP catalog and per-chat context (functional requirements)

MCP connections must be **data-driven**. Adding a new MCP server URL must not require changing Java source, rebuilding, or redeploying the application.

| ID | Requirement | Acceptance |
|---|---|---|
| REQ-MCP-09 | **Runtime catalog** — users/admins register MCP connections via UI or REST (`POST /api/v1/mcp/connections`) | New connection appears in catalog; client initializes and discovers tools without redeploy |
| REQ-MCP-10 | **Persistent definitions** — catalog stored outside compiled code (DB table `ai_chat.mcp_connection` or equivalent) | Restart reloads catalog; optional seed from `application.yml` / env on first boot only |
| REQ-MCP-11 | **Per-chat selection** — each chat stores `enabledMcpConnections` (connection IDs) | `PUT /api/v1/chats/{chatId}/mcp` updates selection; persisted per session |
| REQ-MCP-12 | **UI picker** — chat interface shows available connections; user toggles which are active for **current chat** | Composer or sidebar MCP panel; changes apply to next message turn |
| REQ-MCP-13 | **Scoped tool injection** — `MCPToolAdvisor` uses intersection of (catalog UP servers) ∩ (chat-enabled IDs) | Disabled or unselected servers never expose tools for that chat |
| REQ-MCP-14 | **No compile for new URL** — forbidden to require new `spring.ai.mcp.client.sse.connections.*` YAML keys as the only way to add servers in production | YAML/env may bootstrap defaults; runtime API/UI is the primary extension path |

**Invariant:** chat without any MCP selected or reachable still streams LLM replies (unchanged degradation rules).

### MCP connection model

The application is an **MCP client** that connects to one or more MCP servers over SSE transport. It uses Spring AI's `McpSyncClient` to discover tools, resources, and prompts from each server.

| Property | Description |
|---|---|
| Transport | SSE (Server-Sent Events) |
| Client type | `McpSyncClient` (SYNC) |
| Connection config | Runtime catalog (DB) + optional bootstrap in `application.yml` / env |
| Per-chat scope | `enabledMcpConnections` on `Chat`; advisor filters by chat + health |
| Auto-discovery | On connect/reconnect, discover tools/resources/prompts per capability flags |
| Dynamic tool selection | LLM decides which tools to invoke from **chat-enabled** reachable servers |

### MCP server connections (configurable)

**Bootstrap (optional)** — seed default connections at first deploy:

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
```

**Runtime catalog (required for REQ-MCP-09)** — additional connections added via `POST /api/v1/mcp/connections` or chat UI; persisted to `ai_chat.mcp_connection`. No application rebuild.

Example runtime payload:

```json
{
  "name": "weather-service",
  "url": "http://localhost:8093/sse",
  "tools": true,
  "resources": false,
  "prompts": false
}
```

### MCP server discovery flow (startup)

1. Load connection definitions from **persistent catalog** (and merge optional YAML bootstrap seeds if catalog empty)
2. For each catalog entry, create or refresh `McpSyncClient` via `HttpClientSseClientTransport` (`GET /sse`, `POST /mcp/message`)
3. Call `initialize()` → store server name, version, capabilities
4. Call `listTools()`, `listResources()`, `listPrompts()` per capability flags
5. Register results in `McpServerRegistry` (in-memory catalog with health status per server)
6. Wrap tools as `McpToolCallbackWrapper` → Spring AI `ToolCallback` (prefixed `[MCP:{serverName}]`)
7. Expose registry via actuator health (`McpConnectionHealthIndicator`)
8. On `POST /api/v1/mcp/connections`, repeat steps 2–7 for the new entry **without restart**

### MCP tool selection flow (per turn)

1. Resolve **chat-enabled** connection IDs (`Chat.enabledMcpConnections` or `GET` equivalent)
2. `MCPToolAdvisor` reads `McpServerRegistry.getReachableServers()` filtered to that set — only UP servers in the intersection
3. Builds system-text catalog: server name, tool name, description, input schema summary
4. Attaches filtered tool callbacks to the prompt
5. Primary model (`gemma4:31b-cloud`) reasons over user message + session memory + tool catalog
6. `ToolCallingAdvisor` delegates actual tool invocation to `functiongemma:270m`
7. Tool results flow back into the chat model context; `ChatStreamActivityPublisher` emits `activity/tool_call` SSE events
8. Final response streams as `token` events

**Server selection rule:** only MCP connections **enabled for the current chat** and **reachable in the registry** are offered to the LLM. The model selects tools by name/description relevance — no hard-coded routing table in Java.

### Graceful degradation (required)

**Invariant:** users can always send messages and receive streamed LLM responses regardless of MCP availability.

| Condition | Behavior |
|---|---|
| MCP server unreachable at startup | Log warning; mark server DOWN in registry; **application starts**; chat works without tools |
| MCP server fails mid-session | Tool call returns error to LLM; activity panel shows failure; turn continues with LLM-only response |
| No MCP servers configured | `MCPToolAdvisor` is no-op; standard chat without enrichment |
| All configured servers DOWN | Same as above — LLM answers from session memory only |
| `ai-chat.features.mcp-client: false` | Skip MCP client beans entirely; chat unchanged |
| Ollama available, MCP not | Normal chat operation (only external hard dependency is LLM + DB) |

### Required MCP integration: `ai-architect-6-mcp` (phase 2)

Phase 2 deliverable: connect to [ai-architect-6-mcp](https://github.com/berdachuk/ai-architect-6-mcp) (`medical-mcp-server`, port `8092`) **when available**. This is a feature milestone, not a runtime hard dependency — chat must work without it.

**Available tools from ai-architect-6-mcp (`medical-mcp-server`):**

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

Same state machine as med-expert-match-ce `DoctorMatchWorkflowState`:

```text
TASK_CREATED → PLANNING → CONTEXT_BUILT → TOOLS_EXECUTED → VERIFYING → POLICY_GATE → DONE
                                                                                    ↘ NEEDS_HUMAN
                                                                                    ↘ FAILED
```

Each transition emits `pipeline_stage` SSE. Tool calls and LLM invocations emit `activity` SSE via `ChatStreamActivityPublisher`.

### Harness components (new — generic, not medical)

| Component | Role | Reference equivalent |
|---|---|---|
| `ChatWorkflowEngine` | Per-turn orchestrator: plan → context → tools → verify → policy → stream | Replaces `DoctorMatchWorkflowEngine` etc. |
| `AgentPlannerService` | Builds step plan using `functiongemma` + structured output | Same pattern as reference `AgentPlannerService` |
| `AgentResponseVerifier` | Checks tool outputs against plan acceptance criteria | Same pattern |
| `PolicyGateService` | Basic safety/quality gate (non-empty response, no refusal-without-explanation) | Simplified `MedicalAgentPolicyGateService` |
| `ChatStreamActivityPublisher` | Bridges Spring events → SSE `activity` events | **Port** from `ChatStreamActivityPublisherImpl` |
| `HarnessWorkflowRunStore` | Persists workflow run state (JDBC) | Same pattern |
| `HarnessChainTraceStore` | Traces chained workflow events | Same pattern |

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

Ported from med-expert-match-ce `chat.js` (agent panel + collapsible summary). The frontend receives SSE events and renders an **Agent Progress Panel** per turn:

| SSE `event:` | `activity.type` or payload | Frontend display |
|---|---|---|
| `agent` | `type: agent_start` | Agent name + start timestamp |
| `agent` | `type: agent_done` | Collapse panel with step count + elapsed time |
| `activity` | `tool_call` | `toolName` + optional arguments (`<pre>`) |
| `activity` | `reasoning` | Collapsible `<details>` with reasoning text |
| `activity` | `todo_update` | Plan steps with status badges |
| `activity` | `llm_call` | Model, token counts, latency |
| `activity` | `llm_turn_summary` | Turn rollup (total tokens, call count) |
| `pipeline_stage` | `{stage, agent, status, timestampMs}` | Stage row with ✓/▶/✗ icon |
| `token` | `{t: "<chunk>"}` | Append to streaming assistant bubble (Markdown) |
| `done` | `{id, content, tokensUsed?}` | Finalize message, refresh sidebar counts |

Optional (phase 1.5): parallel `EventSource` on `/api/v1/logs/stream?sessionId=` for execution log lines (reference pattern).

---

## 7. LLM Connections

### OpenAI-compatible client (default: Ollama)

All LLM access uses the **Spring AI OpenAI-compatible client** — same pattern as [`med-expert-match-ce`](https://github.com/berdachuk/med-expert-match-ce):

| Requirement | Specification |
|---|---|
| Client library | `spring-ai-starter-model-openai` → `OpenAiChatModel`, `OpenAiApi`, `ChatClient` |
| Factory | `OpenAiChatModelFactory` builds models from `spring.ai.custom.*` properties |
| Auto-configuration | **Disabled** — `OpenAiChatAutoConfiguration` and related OpenAI auto-config beans excluded; manual wiring only |
| Config `provider` value | `openai` (Spring AI convention for OpenAI-compatible HTTP API, not necessarily OpenAI cloud) |
| **Default backend** | **Ollama** — `http://localhost:11434/v1` for all roles unless overridden |
| Default API key | `none` (Ollama does not require a key) |
| URL normalization | Factory appends `/v1` suffix when missing (Ollama compatibility) |
| Swappable backends | Any OpenAI-compatible endpoint per role: LM Studio, vLLM, OpenAI, Azure OpenAI, etc. — change `*_BASE_URL`, `*_API_KEY`, `*_MODEL` env vars only |
| Bean lifecycle | All `ChatModel` beans `@Lazy` — no connection at startup unless validated explicitly |

**Default Ollama endpoints (all roles):**

| Role | Env prefix | Default `base-url` | Default model |
|---|---|---|---|
| Chat (primary) | `CHAT_*` | `http://localhost:11434/v1` | `gemma4:31b-cloud` |
| Chat (alt) | `CHAT_ALT_*` | `http://localhost:11434/v1` | `gemma4:12b` |
| Tool calling | `TOOL_CALLING_*` | `http://localhost:11434/v1` | `functiongemma:270m` |

Example — point chat at a cloud OpenAI-compatible API while keeping Ollama for tool calling:

```yaml
# Override via environment (no code change)
CHAT_BASE_URL=https://api.example.com/v1
CHAT_API_KEY=sk-...
CHAT_MODEL=gpt-4o
TOOL_CALLING_BASE_URL=http://localhost:11434/v1   # still Ollama
TOOL_CALLING_MODEL=functiongemma:270m
```

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

- `OpenAiChatModelFactory.create()` builds `OpenAiChatModel` via `OpenAiApi.builder().baseUrl(...).apiKey(...)`
- All ChatModels are `@Lazy` to avoid initialization overhead
- Auto-config disabled for: `OpenAiChatAutoConfiguration`, `OpenAiEmbeddingAutoConfiguration`, `OpenAiAudioSpeechAutoConfiguration`, `OpenAiAudioTranscriptionAutoConfiguration`, `OpenAiImageAutoConfiguration`
- **Default:** all roles target Ollama OpenAI-compatible API (`/v1`); other providers supported by configuration only

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
│            │  MCP: [☑ medical-dataset] [☐ weather] [+ Add] │
│  [Delete   │  Composer: [textarea] [Send]               │
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

### MCP connection UI (REQ-MCP-12)

- **Catalog panel** — list available MCP connections (name, status UP/DOWN, tool count)
- **Add connection** — form: name, SSE URL, capability toggles; submits to `POST /api/v1/mcp/connections` (no redeploy)
- **Per-chat toggles** — checkboxes (or chips) for which connections are active in the **current chat**; saves via `PUT /api/v1/chats/{chatId}/mcp`
- **Visual feedback** — show which MCPs contributed to the current turn in the agent panel when tools run

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
CREATE INDEX idx_chat_user_activity ON ai_chat.chat (user_id, last_activity_at DESC);

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
CREATE INDEX idx_chat_message_chat ON ai_chat.chat_message (chat_id, created_at);

-- Spring AI Session JDBC tables (managed by spring-ai-starter-session-jdbc)
-- ai_session, ai_session_event — auto-created by Spring AI

-- Harness workflow tables — see V2__harness_schema.sql (03-design.md)

-- MCP runtime catalog + per-chat bindings — see V2__mcp_catalog.sql (REQ-MCP-09–11)
-- ai_chat.mcp_connection (id, name, url, capability flags, created_at, …)
-- ai_chat.chat_mcp_binding (chat_id, connection_id) or chat.enabled_mcp_connections JSONB column
```

### ID generation

24-char hex strings via `IdGenerator.generateId()` — same MongoDB ObjectId-compatible format as `med-expert-match-ce`.

---

## 10. Architecture (Spring Modulith)

**Canonical reference:** [02-architecture.md](02-architecture.md)

Single deployable Spring Boot application using **package-based modules**. Each module has a `package-info.java` annotated with `@ApplicationModule(allowedDependencies = …)`.

### Package modules

```
src/main/java/com/berdachuk/aichat/
├── AiChatApplication.java
├── core/              # Shared config, security, health, util
├── chat/              # Chat domain, repository, service, REST controller
├── llm/               # LLM orchestration, harness, tools, advisors
├── mcp/               # MCP client config, tool discovery, tool wrappers
├── web/               # Thymeleaf SSR web controllers
└── system/            # Actuator health indicators
```

### Module dependency graph

```text
web     → core, chat, llm
chat    → core
llm     → core, chat, mcp
mcp     → core
system  → core, mcp
```

### Interface / implementation rules

| Layer | Public API | Implementation |
|---|---|---|
| Repository | `{module}/repository/XxxRepository.java` | `{module}/repository/impl/XxxRepositoryImpl.java` |
| Service | `{module}/service/XxxService.java` | `{module}/service/impl/XxxServiceImpl.java` |
| MCP adapters | `mcp/*` — delegates to service interfaces | No JDBC in MCP layer |

### SQL persistence rules (mandatory)

Pattern reference: [`MedicalCaseRepositoryImpl`](https://github.com/berdachuk/ai-architect-6-mcp/blob/main/src/main/java/com/example/medicalmcp/medicalcase/repository/impl/MedicalCaseRepositoryImpl.java) in **ai-architect-6-mcp** (same as med-expert-match-ce / ai-chat `core.repository.sql.InjectSql`).

| Rule | Requirement |
|---|---|
| **No inline SQL** | SQL text must **not** appear in Java string literals (`"SELECT …"`, text blocks, or concatenation). All statements live in `src/main/resources/sql/<module>/*.sql`. |
| **Inject SQL** | Repository `impl` classes declare `@InjectSql("/sql/<module>/<name>.sql")` fields; `SqlInjectBeanPostProcessor` in `core` loads classpath content at startup. |
| **Named parameters only** | SQL files use `:bindName` placeholders (e.g. `WHERE id = :id`). Java passes values via `MapSqlParameterSource` / `Map.of("id", id)` with `NamedParameterJdbcTemplate` — **never** positional `?` placeholders or `JdbcTemplate` for application SQL. |
| **One file per statement** | Prefer one logical query per `.sql` file (`selectById.sql`, `insert.sql`, `listByUser.sql`). |
| **Flyway separate** | Schema DDL/migrations stay in `db/migration/`; runtime DML/SELECT in `sql/` — do not mix. |

Example SQL (`sql/chat/selectById.sql`):

```sql
SELECT id, user_id, name, agent_id, is_default, created_at, updated_at, last_activity_at, message_count
FROM ai_chat.chat
WHERE id = :id
```

Example repository field:

```java
@InjectSql("/sql/chat/selectById.sql")
private String selectByIdSql;
```

---

## 11. Key Dependencies

### 11.1 Version policy (mandatory)

| Rule | Requirement |
|---|---|
| **Spring Boot** | **4.x only** — parent `spring-boot-starter-parent` at latest stable **4.x** patch (baseline **4.1.0**). Do not use Spring Boot 3.x for new work. |
| **Spring AI** | **2.0.0** BOM (`spring-ai-bom`) — all `spring-ai-*` artifacts from this BOM; use latest stable **2.0.x** patch resolved by the BOM. |
| **Spring Modulith** | **2.1.x** BOM — compatible with Boot 4.x |
| **Other libraries** | Prefer **latest stable** versions compatible with Boot 4 / Spring AI 2.0 (PostgreSQL driver, Flyway, Testcontainers, etc. via Boot BOM or explicit current stable) |
| **Upgrades** | When bumping dependencies, run `mvn test` and `mvn verify -Pintegration`; record material changes in `decisions.md` |

`pom.xml` must import `spring-ai-bom` **2.0.0** even before LLM features land (M3), so the stack is fixed from M1 onward.

### 11.2 Dependency list

| Dependency | Version | Notes |
|---|---|---|
| Spring Boot | 4.1.0 | Parent POM (latest stable 4.x) |
| Spring AI BOM | 2.0.0 | Mandatory AI stack |
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
    url: jdbc:postgresql://${AICHAT_DB_HOST:localhost}:${AICHAT_DB_PORT:5437}/${AICHAT_DB_NAME:ai_chat}?currentSchema=ai_chat
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
  port: ${SERVER_PORT:8095}
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
| `AICHAT_DB_PORT` | `5437` | PostgreSQL port |
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
| `MCP_MEDICAL_URL` | `http://localhost:8092/sse` | ai-architect-6-mcp SSE endpoint (`medical-dataset` connection) |
| `SERVER_PORT` | `8095` | Application port |

---

## 14. Milestones

### Phase 1 — Core chat (no MCP required)

| # | Milestone | Key deliverables | Acceptance criteria | Status |
|---|---|---|---|---|
| M1 | Schema + modulith foundation | `V1__init_chat_schema.sql`, domain records, `package-info.java`, Boot stub, `ModulithArchitectureTest` | `mvn test` passes; schema migrates | ✅ |
| M2 | Chat session CRUD | `ChatRepository`, `ChatService`, `ChatController` REST | Create/list/rename/delete/history; default chat auto-created | ✅ |
| M3 | LLM integration | `SpringAIConfig`, `OpenAiChatModelFactory`, `ChatAssistantService`, SSE `token`/`done` | Stream response from Ollama; **works with MCP disabled** | ✅ |
| M4 | Session memory | Session JDBC, compaction, `SessionMemoryAdvisor`, `DateTimeContextAdvisor` | 20+ turn dialog retains context; compaction fires at threshold | ✅ |
| M5 | Harness engine | `ChatWorkflowEngine`, planner/verifier/policy, `ChatStreamActivityPublisher` | Agent panel shows `pipeline_stage` + `activity` events | ✅ |
| M6 | Frontend | `chat.html`, `chat.js`, sidebar, composer, agent panel | Full chat UX in browser; session switch/delete works | ✅ |

### Phase 2 — MCP enrichment

| # | Milestone | Key deliverables | Acceptance criteria | Status |
|---|---|---|---|---|
| M7 | MCP client + runtime catalog | `McpClientConfig`, `McpServerRegistry`, `MCPToolAdvisor`, `mcp_connection` table, REST `/api/v1/mcp/connections` | REQ-MCP-09/10/14; add connection via API without redeploy; WireMock IT | ⬜ |
| M8 | Per-chat MCP + ai-architect-6-mcp | Chat `enabledMcpConnections`, UI toggles, `PUT .../mcp` | User enables medical-dataset for chat → tools from `:8092`; REQ-MCP-11–13 | ⬜ |

### Phase 3 — Packaging

| # | Milestone | Key deliverables | Acceptance criteria | Status |
|---|---|---|---|---|
| M9 | Docker + polish | `docker-compose.yml`, `Dockerfile`, integration tests, smoke checklist | `docker compose up` healthy; manual smoke checklist complete | ⬜ |

---

## 15. Docker Compose

```yaml
services:
  ai-chat:
    build: .
    ports:
      - "8095:8095"
    environment:
      AICHAT_DB_HOST: postgres
      AICHAT_DB_USERNAME: ai_chat
      AICHAT_DB_PASSWORD: ai_chat
      CHAT_BASE_URL: http://host.docker.internal:11434/v1
      CHAT_API_KEY: none
      CHAT_MODEL: gemma4:31b-cloud
      CHAT_ALT_BASE_URL: http://host.docker.internal:11434/v1
      CHAT_ALT_API_KEY: none
      CHAT_ALT_MODEL: gemma4:12b
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
      test: ["CMD", "curl", "-f", "http://localhost:8095/actuator/health"]
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
      test: ["CMD-SHELL", "pg_isready -U ai_chat -d ai_chat"]
      interval: 10s
      timeout: 5s
      retries: 5

volumes:
  pgdata:
```

Ollama runs on the host (`host.docker.internal:11434`), not in Docker.

---

## Related documentation

- [README.md](README.md) — documentation index and naming
- [../README.md](../README.md) — project overview
- [02-architecture.md](02-architecture.md) — system design, Modulith layout, stack
- [03-design.md](03-design.md) — detailed design and class sketches
- [04-testing.md](04-testing.md) — test strategy
- [05-deployment.md](05-deployment.md) — config, Docker, env vars
