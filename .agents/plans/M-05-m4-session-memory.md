# M-05 — M4 Session Memory + Advisors

**Status:** Active  
**Date:** 2026-06-17  
**Milestone:** M4 per [docs/01-requirements.md §14](../../docs/01-requirements.md)

## Goal

Spring AI Session JDBC with turn-window compaction and advisor chain for chat context.

## Deliverables

- [ ] `spring-ai-starter-session-jdbc` + Flyway session schema
- [ ] `AgentSessionProperties` (`spring.ai.session.jdbc.*`)
- [ ] `DateTimeContextAdvisor` in `core`
- [ ] `SessionMemoryAdvisor` wired in `ChatAssistantService`
- [ ] Session ID format `{userId}-{chatId}`
- [ ] IT: multi-turn dialog retains context (stub or Testcontainers)

## Out of scope (M5+)

- Harness workflow engine
- Thymeleaf UI (M6)
- MCP advisors
