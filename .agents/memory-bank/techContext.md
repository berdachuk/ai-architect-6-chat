# Tech context

## Stack

| Layer | Choice |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot **4.1.0** (4.x latest stable), Spring AI BOM **2.0.0**, Spring Modulith **2.1.0** |
| DB | PostgreSQL 17, Flyway, JDBC only |
| UI | Thymeleaf 3, Bootstrap 5.3, vanilla JS (planned M6) |
| LLM client | `spring-ai-starter-model-openai` → `OpenAiChatModel` (M3+) |
| LLM default | Ollama `http://localhost:11434/v1` |
| MCP client | `spring-ai-starter-mcp-client` 2.0.0 (phase 2) |
| Base package | `com.berdachuk.aichat` |
| REST docs | springdoc-openapi + OpenAPI Generator (M2+) |

## Version policy

See [docs/01-requirements.md §11.1](../../docs/01-requirements.md) — Boot **4.x**, Spring AI **2.0.0** BOM mandatory; latest stable patches.

## Commands (when implemented)

```bash
mvn test                    # unit + Modulith
mvn verify -Pintegration    # Testcontainers IT (Docker required; Windows: use WSL per DEC-008)
mvn spring-boot:run         # local :8095
```

## Development (DEC-008, DEC-009)

- **Windows:** Docker via WSL 2; run Maven/Docker from WSL.
- **Integration tests:** Testcontainers only — no manual Postgres for IT.

## Local dependencies

- PostgreSQL (`ai_chat` / user `ai_chat`)
- Ollama with models pulled
- MCP (optional): ai-architect-6-mcp on `:8092`

## Key env vars

`CHAT_*`, `CHAT_ALT_*`, `TOOL_CALLING_*`, `AICHAT_DB_*`, `MCP_MEDICAL_URL`, `SERVER_PORT`

Full list: [docs/05-deployment.md](../../docs/05-deployment.md)

## Git remote (berdachuk)

`git@github.com-berdachuk:berdachuk/ai-architect-6-chat.git`

## Test stack

JUnit 5, Spring Modulith test, Testcontainers PG 17, WireMock for MCP IT.
