# MCP Tool Calling — Model Comparison Report

**Date:** 2026-06-18  
**Test:** 7-step medical dataset workflow (explore → search → get → analyze)  
**MCP server:** `ai-architect-6-mcp` (5 tools, 1 prompt, 2 resources)  
**Client:** `ai-chat` with `McpToolCallbackWrapper` fixes applied

## Test workflow

| Step | User message | Expected MCP call |
|------|-------------|-------------------|
| 1 | "What medical dataset stats are available?" | `get_dataset_stats` |
| 2 | "List all medical specialties and their case counts." | `list_specialties` |
| 3 | "Search for cases about pacemaker interrogation." | `search_cases` |
| 4 | "Find cases semantically similar to: pacemaker device check." | `semantic_search` |
| 5 | "Get the full case details for ID `<from-step-3>`." | `get_case` |
| 6 | "Use the case-analysis prompt for that case with focus transcription." | `case-analysis` prompt |
| 7 | "Run case-analysis on the same case with focus specialty." | `case-analysis` + `PREDICTED_LABEL` |

## Results

| Model | Size | Steps 1-5 (tools) | Step 6 (prompt) | Step 7 (classification) | Notes |
|-------|------|-------------------|-----------------|------------------------|-------|
| **minimax-m3:cloud** | ~? | ✅ All correct | ✅ Applied directly | ✅ `PREDICTED_LABEL` | Best overall. Recognized prompt as template, not tool. |
| **gemma4:12b** | 12B | ✅ All correct | ✅ Applied directly | ✅ `PREDICTED_LABEL` | Required hyphen→underscore fix in tool names. |
| **gemma4:e4b** | ~4B | ✅ All correct | ❌ Tried to invoke as tool | ❌ Same error | Smaller model. Ignores "prompt templates — not invocable tools" label. |
| **qwen3.5:9b** | 9B | ✅ All correct (step 3 used `semantic_search` instead) | ✅ Applied directly | ❌ No `PREDICTED_LABEL` | Good tool selection but doesn't follow prompt template structure for classification. |
| **gemma4:31b-cloud** | 31B | ❌ Called `get_case` without `id` | ❌ | ❌ | Original default. Root cause was tool naming bug (all tools named "medical-dataset"). |

## Key findings

### 1. Tool naming is critical
The original `McpToolCallbackWrapper` set `prefixedToolName` to just the server name (`"medical-dataset"`), making all 5 tools indistinguishable. Fix: `serverName + "_" + tool.name()`.

### 2. Hyphens in tool names break some models
`gemma4:12b` normalizes hyphens to underscores (`medical-dataset` → `medical_dataset`). Fix: `serverName.replace("-", "_")` in `McpToolCallbackWrapper`.

### 3. Prompt templates vs. tools
Smaller models (`gemma4:e4b`) try to invoke prompts as tools despite the catalog labeling them "prompt templates — not invocable tools". Larger models (`minimax-m3:cloud`, `gemma4:12b`) correctly recognize prompts as templates and apply them directly.

### 4. Classification block generation
Only `minimax-m3:cloud` and `gemma4:12b` reliably generate the `PREDICTED_LABEL` classification block when asked for `focus=specialty`. `qwen3.5:9b` provides analysis but omits the structured block.

### 5. Tool selection accuracy
All models correctly select the right tool for steps 1-5 after the naming fixes. `qwen3.5:9b` used `semantic_search` for step 3 instead of `search_cases` — a valid alternative but not the expected choice.

## Recommendations

| Priority | Action |
|----------|--------|
| **High** | Default model: `gemma4:12b` or `minimax-m3:cloud` — both pass all 7 steps |
| **Medium** | Keep `gemma4:31b-cloud` as fallback only — too large, poor tool discipline |
| **Low** | `qwen3.5:9b` acceptable for steps 1-6, skip step 7 classification |
| **Avoid** | `gemma4:e4b` — too small for prompt template recognition |

## Code fixes applied during testing

| Fix | File | Reason |
|-----|------|--------|
| Unique tool names | `McpToolCallbackWrapper.java` | All tools had same name `"medical-dataset"` |
| Hyphen→underscore | `McpToolCallbackWrapper.java` | `gemma4:12b` normalizes hyphens |
| Prompt label | `McpServerRegistry.java` | Clarify prompts are templates, not tools |
| `conversationHistoryEnabled` | `LlmChatClientConfiguration.java` | Preserve system message during tool calls |
| `ClassCastException` | `MCPToolAdvisor.java` | `DefaultToolCallingChatOptions` vs `OpenAiChatOptions` |
| `inputSchema` in catalog | `McpServerRegistry.java` | LLM needs parameter schemas |
| `instructions` capture | `McpClientConnector.java` + `McpServerInfo.java` | MCP server workflow narrative |
