# M-04 — M3 LLM Integration + SSE

**Status:** Active  
**Date:** 2026-06-17  
**Milestone:** M3 per [docs/01-requirements.md §14](../../docs/01-requirements.md)

## Goal

Wire Spring AI 2.0 with OpenAI-compatible Ollama client; stream assistant replies via SSE on the chat message endpoint.

## Deliverables

- [ ] `spring-ai-starter-model-openai` (or equivalent) in `pom.xml`
- [ ] `llm/config/SpringAIConfig` + `OpenAiChatModelFactory` (Ollama default `http://localhost:11434/v1`)
- [ ] `ChatAssistantService` + `ChatAssistantServiceImpl` — `processMessage`, `streamMessage`
- [ ] `ChatController` — `POST /api/v1/chats/{chatId}/messages/stream` (SSE `token`/`done`)
- [ ] Append user/assistant messages via `ChatService`; degrade when LLM unreachable
- [ ] Unit tests for factory/config; IT with mocked or Testcontainers Ollama (optional skip tag)

## TDD / traceability

| ID | Behavior | Module |
|---|---|---|
| M3 | SSE streaming from Ollama | `llm`, `chat` |
| DEC-012 | Boot 4.x + Spring AI 2.0.0 | `llm` |

## Acceptance

- POST stream returns SSE events; chat history persists user + assistant messages
- App starts and chats work when Ollama is down (graceful error SSE or 503)

## Dependencies

- M2 complete (chat CRUD, repositories, REST)

## Out of scope (M4+)

- Harness workflow engine, session JDBC advisors
- Thymeleaf UI
- MCP tool calling
