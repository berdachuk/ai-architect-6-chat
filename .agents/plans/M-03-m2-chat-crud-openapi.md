# M-03 — M2 Chat CRUD + OpenAPI

**Status:** Active  
**Date:** 2026-06-17  
**Milestone:** M2 per [docs/01-requirements.md §14](../../docs/01-requirements.md)

## Goal

Chat session persistence and REST API with OpenAPI 3 documentation and generated client for integration tests.

## Deliverables

- [ ] `ChatRepository` + `ChatRepositoryImpl` (JDBC, `@InjectSql`, SQL in `resources/sql/chat/`)
- [ ] `ChatService` + `ChatServiceImpl` (default chat lifecycle, `requireOwnedChat`)
- [ ] `ChatController` — `/api/v1/chats` CRUD + history
- [ ] springdoc-openapi + Swagger UI (`/v3/api-docs`, `/swagger-ui`)
- [ ] `openapi-generator-maven-plugin` — Java client for IT
- [ ] `ChatControllerIntegrationTest` via generated client (Testcontainers, DEC-009)
- [ ] Security permit-all for `/api/v1/**`, swagger paths (local/dev)

## TDD / traceability

| ID | Behavior | Module |
|---|---|---|
| M2 | List/create/rename/delete/history chats | `chat` |
| DEC-007 | OpenAPI spec + generated IT client | `chat`, tests |

## Acceptance

- CRUD + default chat recreation works
- `/v3/api-docs` documents all M2 endpoints
- IT uses generated client, not hand-written URLs

## Dependencies

- M1 complete (schema, domain records, Modulith boundaries)

## Out of scope (M3+)

- LLM streaming, SSE message endpoint
- Thymeleaf UI
