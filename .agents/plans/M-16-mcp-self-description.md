# M-16 — MCP Self-Description Protocol

**Status:** Draft  
**Date:** 2026-06-18  
**Tracked in:** `docs/mcp-self-description-improvements.md`

## Goal

Make MCP tools discoverable and usable by generic chat clients by using two standard MCP protocol features: `instructions` (workflow narrative) and prompt argument exposure.

## Problem

`ai-chat` sees 5 MCP tools, 2 resources, and 1 prompt as isolated functions. It has no way to know the intended workflow order (explore → search → get → analyze) or that `get_case` requires a UUID from `search_cases` first. The LLM repeatedly calls `get_case` without the required `id` parameter.

## Solution

| Component | Change | Files |
|-----------|--------|-------|
| **MCP server** | Set `serverInstructions()` on `WebMvcSseServerTransportProvider` | `McpServerConfig.java` (in `ai-architect-6-mcp`) |
| `ai-chat` | Add `instructions` field to `McpServerInfo` record | `McpServerInfo.java` |
| `ai-chat` | Capture `client.getServerInstructions()` after initialize | `McpClientConnector.java` |
| `ai-chat` | Include instructions before tool list in catalog | `McpServerRegistry.java` |
| `ai-chat` | Expose prompts with argument schemas in catalog | `McpServerRegistry.java` |

## Tasks

### 1. MCP server: set `serverInstructions`

In `ai-architect-6-mcp`, configure the `WebMvcSseServerTransportProvider` bean with `.serverInstructions(...)` containing the workflow narrative:

- Step 1 — Explore: `get_dataset_stats`, `list_specialties`
- Step 2 — Search: `search_cases`, `semantic_search`
- Step 3 — Retrieve: `get_case`
- Step 4 — Analyze: `case-analysis` prompt with `focus` parameter
- Important notes about UUID/hex ID requirements

### 2. ai-chat: extend `McpServerInfo`

Add `String instructions` field to the record. Update all call sites (registry register calls, health view, tests).

### 3. ai-chat: capture instructions

In `McpClientConnector.connect()`, after `client.initialize()`, call `client.getServerInstructions()` and pass it to `registry.register()`.

### 4. ai-chat: include instructions in catalog

In `McpServerRegistry.formatServerCatalog()`, prepend `server.instructions()` before the tool list.

### 5. ai-chat: expose prompts in catalog

After the tool list in `formatServerCatalog()`, append a `#### Prompts` section with prompt name, description, and argument schemas (name, required, description).

## Result

The LLM sees the full workflow narrative plus tool parameter schemas in its system message:

```markdown
### medical-dataset (medical-mcp-server)
## Workflow

### Step 1 — Explore the dataset
Ask about available stats or specialties.

### Step 2 — Search for cases
Use `search_cases` or `semantic_search` to find relevant cases.

### Step 3 — Retrieve full details
Use `get_case` with a UUID from Step 2.

### Step 4 — Analyze with prompt
Use `case-analysis` prompt with a UUID and optionally `focus`.

### Important
- `get_case` requires a valid 24-char hex ID from search results.
- `case-analysis` prompt also requires a valid case ID.

- **get_dataset_stats**: Return dataset statistics...
  - Parameters: none
- **list_specialties**: List all medical specialties...
  - Parameters: none
- **search_cases**: Full-text search...
  - Parameters: query (string, required), specialty (string), split (string), limit (integer)
- **semantic_search**: Vector similarity search...
  - Parameters: query (string, required), specialty (string), topK (integer), minSimilarity (number)
- **get_case**: Retrieve a single medical case by UUID...
  - Parameters: id (string, required)
- **get_dataset_stats**: Get dataset stats...
  - Parameters: none

#### Prompts
- **case-analysis**: Structured prompt for LLM analysis...
  - caseId (required): Server UUID from search_cases / semantic_search
  - focus: Dataset field emphasis...
```

## Dependencies

- Task 1 must be deployed and running before Tasks 2–5 can be verified.
- Tasks 2–5 touch `mcp/` module only; no changes to `chat/`, `llm/`, or `web/`.

## Verification

1. Start MCP server with `serverInstructions`
2. Start ai-chat with `dev,debug` profiles
3. Check log: `MCP connection 'medical-dataset' initialized: 5 tools, 1 resources, 1 prompts`
4. Send a chat message like "What medical dataset stats are available?"
5. Verify the LLM is not calling `get_case` without an ID
