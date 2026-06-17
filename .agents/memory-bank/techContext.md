# Tech context

## Stack

| Layer | Choice |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.1.0, Spring AI 2.0.0, Spring Modulith 2.1.0 |
| DB | PostgreSQL 17, Flyway, JDBC only |
| UI | Thymeleaf 3, Bootstrap 5.3, vanilla JS |
| LLM client | `spring-ai-starter-model-openai` → `OpenAiChatModel` |
| LLM default | Ollama `http://localhost:11434/v1` |
| Models | `gemma4:31b-cloud`, `gemma4:12b`, `functiongemma:270m` |
| Session memory | `spring-ai-starter-session-jdbc` 0.3.0 |
| MCP client | `spring-ai-starter-mcp-client` 2.0.0 (phase 2) |
| Base package | `com.berdachuk.aichat` |

## Commands (when implemented)

```bash
mvn test                    # unit + Modulith
mvn verify -Pintegration    # Testcontainers IT
mvn spring-boot:run         # local :8080
```

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
