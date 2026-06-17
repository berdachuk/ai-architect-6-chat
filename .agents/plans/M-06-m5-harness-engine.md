# M-06 — M5 Harness Workflow Engine

**Status:** Active  
**Date:** 2026-06-17  
**Milestone:** M5 per [docs/01-requirements.md §14](../../docs/01-requirements.md)

## Goal

Harness workflow engine with activity SSE events for agent progress visualization.

## Deliverables

- [ ] `ChatWorkflowEngine` / `ChatWorkflowEngineImpl` (generic, no medical domain)
- [ ] `ChatStreamActivityPublisher` + `ChatStreamActivityPublisherImpl`
- [ ] Planner / verifier / policy services (stub or minimal for M5)
- [ ] Wire harness into `ChatAssistantService.streamMessage`
- [ ] SSE events: `activity`, `pipeline_stage`, `agent`
- [ ] IT: stream returns harness activity events (stub workflow)

## Out of scope (M6+)

- Thymeleaf UI (M6)
- MCP advisors (M7)
