# M-05 — M4 Session Memory + Advisors

**Status:** Archived (implemented 2026-06-17)  
**Milestone:** M4

## Delivered

- `spring-ai-starter-session-jdbc` 0.3.0 + Flyway `V2__init_session_schema.sql`
- `AgentSessionProperties`, `SessionMemoryConfiguration` (turn-window compaction)
- `DateTimeContextAdvisor`, `LlmDateTimeContext` in `core`
- `LlmChatClientConfiguration` — advisors on `primaryChatClient`
- `ChatAssistantServiceImpl` — session ID `{userId}-{chatId}`
- IT: `SessionMemoryIntegrationTest`, `DateTimeContextAdvisorTest`
- Default PostgreSQL port `5437` (`AICHAT_DB_PORT`)
