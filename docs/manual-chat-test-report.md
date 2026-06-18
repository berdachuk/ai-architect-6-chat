# Manual Chat Test Report

**Date:** 2026-06-18  
**Model:** `gemma4:31b-cloud` (chat) + `functiongemma:270m` (tool-calling)  
**MCP server:** `ai-architect-6-mcp` (medical-dataset, 5 tools, 1 prompt)

## Test transcript

| # | User message | Expected tool | Actual tool | Result |
|---|-------------|---------------|-------------|--------|
| 1 | "What medical dataset stats are available?" | `get_dataset_stats` | `medical_dataset_get_dataset_stats` | ✅ 2,464 cases, 13 specialties |
| 2 | "List all medical specialties and their case counts." | `list_specialties` | `medical_dataset_list_specialties` | ✅ 13 specialties with counts |
| 3 | "Search for cases about pacemaker interrogation." | `search_cases` | `medical_dataset_search_cases` | ✅ 2 direct matches + 8 related |
| 4 | "Find cases semantically similar to: pacemaker device check." | `semantic_search` | `medical_dataset_semantic_search` + `search_cases` | ✅ No semantic hits, fell back to keyword search |
| 5 | "Get the full case details for ID 6a34458cce31e82c273347d5" | `get_case` | `medical_dataset_get_case` | ❌ Validation error (null keywords field) |
| 5b | "try 6a34458cce31e82c2733484f" | `get_case` | `medical_dataset_get_case` | ❌ Same validation error |
| 5c | "yes" (to suggestion to try interrogation case) | `get_case` | `medical_dataset_get_case` | ✅ Full case retrieved |
| 6 | "Use the case-analysis prompt with focus transcription." | `case-analysis` prompt | Applied directly | ✅ Structured transcription analysis |
| 7 | "Run case-analysis on the same case with focus specialty." | `case-analysis` + `PREDICTED_LABEL` | Applied directly | ✅ `PREDICTED_LABEL: Cardiovascular / Pulmonary` |

## Observations

### Tool selection
All 5 tools were called correctly. Step 4 used `semantic_search` first, got no results, then fell back to `search_cases` — intelligent fallback behavior.

### Prompt template handling
Steps 6-7 correctly recognized `case-analysis` as a prompt template (not a tool) and applied it directly. `PREDICTED_LABEL` classification block generated correctly.

### Data quality issue
Two cases (`6a34458cce31e82c273347d5`, `6a34458cce31e82c2733484f`) failed `get_case` with a validation error — likely null `keywords` field in the MCP server response. The LLM handled this gracefully by suggesting alternative cases.

### Agent panel
All 7 turns showed correct pipeline stages (TASK_CREATED → PLANNING → CONTEXT_BUILT → TOOLS_EXECUTED → VERIFYING → POLICY_GATE → DONE) with tool call activity entries.

## Summary

| Metric | Result |
|--------|--------|
| Tool calls correct | 7/7 (100%) |
| Prompt template recognition | 2/2 (100%) |
| `PREDICTED_LABEL` generated | ✅ |
| Graceful error handling | ✅ (suggested alternatives on failure) |
| Agent panel display | ✅ All stages + tool activity |
