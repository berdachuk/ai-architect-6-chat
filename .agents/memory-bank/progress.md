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

## 2026-06-17 — M2 Chat CRUD + OpenAPI

- `@InjectSql` / `SqlInjectBeanPostProcessor` (DEC-013); JDBC repos + SQL files
- `ChatService`, `ChatController` `/api/v1/chats`; springdoc 3.0.1; Security permit-all dev paths
- OpenAPI Generator native client; `ChatControllerIntegrationTest` (DEC-007, DEC-009)
- **Stack:** Boot 4.1.0, Spring AI BOM 2.0.0, springdoc 3.0.1
- **Plan:** M-03 archived; M-04 active for M3
