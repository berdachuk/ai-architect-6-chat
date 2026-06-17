# Progress log

## 2026-06-17 — Documentation baseline

- Full `docs/` set; OpenAI-compatible client + default Ollama
- Commits on `develop`

## 2026-06-17 — AI context bootstrap

- `AGENTS.md`, `.agents/skills/`, memory bank, `docs/ai-context-strategy.md`
- Base package `com.berdachuk.aichat` (DEC-006)

## 2026-06-17 — M1 Modulith foundation

- `pom.xml`, `AiChatApplication`, Flyway `V1__init_chat_schema.sql`
- Domain `Chat`, `ChatMessage`; `IdGenerator`; six module `package-info.java`
- Tests: `ModulithArchitectureTest`, `IdGeneratorTest`, `FlywaySchemaIntegrationTest`
- **Commands:** `mvn test`, `mvn verify -Pintegration` — pass
- **Plan:** M-02 archived; M-03 active for M2

## 2026-06-17 — M3 LLM integration + SSE

- Spring AI 2.0 OpenAI-compatible client (`OpenAiChatModelFactory`, `AiChatProperties`, `SpringAIConfig`)
- `ChatAssistantService` + `ChatStreamController` — SSE `token`/`done`/`error`
- Test profile stub `ChatModel`; `ChatStreamControllerIntegrationTest`
- **Plan:** M-04 archived; M-05 active for M4 session memory

## 2026-06-17 — M4 Session memory + advisors

- `spring-ai-starter-session-jdbc` 0.3.0, Flyway `V2__init_session_schema.sql`
- `DateTimeContextAdvisor`, `SessionMemoryAdvisor`, turn-window compaction
- Session ID `{userId}-{chatId}`; default DB port `5437`
- **Plan:** M-05 archived; M-06 active for M5 harness

