---
name: testing
description: TDD, unit/integration tests, Flyway, Testcontainers for ai-chat
version: "1.0"
tags:
  - testing
  - tdd
  - testcontainers
---

# Testing

## Description

Test strategy for ai-chat per [docs/04-testing.md](../../docs/04-testing.md): unit, Modulith, Testcontainers integration.

## When to use

- Before any implementation (TDD)
- Adding repository, service, controller, SSE tests
- Flyway schema changes
- MCP client integration tests

## Instructions

### TDD workflow (mandatory)

1. Write test first
2. Requirement alignment review (which M#/REQ?, module?, domain models?)
3. Security pre-check if auth/API/DB/input (`security-check` skill)
4. Implement minimal code
5. `mvn test` or `mvn verify -Pintegration`
6. Security post-check on diff

### Pyramid

| Layer | Examples |
|---|---|
| Unit | `ChatServiceImplTest`, `McpServerRegistryTest`, `MCPToolAdvisorTest` |
| Modulith | `ModulithArchitectureTest` — every commit |
| IT | `ChatControllerIntegrationTest`, `McpClientIntegrationTest` (WireMock) |

### Flyway / DB

- `V1__init_chat_schema.sql` — `ai_chat.chat`, `ai_chat.chat_message`
- `V2__harness_schema.sql` — harness tables (M5+)
- IT uses Testcontainers `postgres:17`

### MCP tests

- Mock MCP with WireMock; verify app **starts** when MCP URL dead (`McpUnavailableStartupTest`)

### Java Cucumber (when adopted)

- `.feature` in acceptance-test layer
- Tags: `@m2`, `@req-001`, `@chat`
- Thin step definitions — logic in services

## Boundaries

- Do not test LLM output quality
- Do not require live Ollama in CI
- Do not skip Modulith verify
