# M-06 — M5 Harness Workflow Engine

**Status:** Archived (implemented 2026-06-17)  
**Milestone:** M5

## Delivered

- `ChatWorkflowEngine` / `ChatWorkflowEngineImpl` — pre/post stream stages
- `ChatStreamActivityPublisher` + impl
- Stub `AgentPlannerService`, `AgentResponseVerifier`, `PolicyGateService`
- JDBC `HarnessWorkflowRunStore`, `HarnessChainTraceStore`, Flyway `V3__init_harness_schema.sql`
- SSE: `agent`, `pipeline_stage`, `activity` (todo_update)
- IT: `HarnessStreamIntegrationTest`
