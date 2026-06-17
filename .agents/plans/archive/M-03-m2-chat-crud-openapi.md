# M-03 — M2 Chat CRUD + OpenAPI

**Status:** Completed  
**Date:** 2026-06-17  
**Completed:** 2026-06-17  
**Milestone:** M2 per [docs/01-requirements.md §14](../../docs/01-requirements.md)

## Goal

Chat session persistence and REST API with OpenAPI 3 documentation and generated client for integration tests.

## Deliverables

- [x] `ChatRepository` + `ChatRepositoryImpl` (JDBC, `@InjectSql`, SQL in `resources/sql/chat/`, named `:bind` only per DEC-013)
- [x] `ChatService` + `ChatServiceImpl` (default chat lifecycle, `requireOwnedChat`)
- [x] `ChatController` — `/api/v1/chats` CRUD + history
- [x] springdoc-openapi + Swagger UI (`/v3/api-docs`, `/swagger-ui`)
- [x] `openapi-generator-maven-plugin` — Java client for IT
- [x] `ChatControllerIntegrationTest` via generated client (Testcontainers, DEC-009)
- [x] Security permit-all for `/api/v1/**`, swagger paths (local/dev)

## Acceptance

- CRUD + default chat recreation works
- `/v3/api-docs` documents all M2 endpoints
- IT uses generated client, not hand-written URLs
