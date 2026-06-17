# Active context

**Updated:** 2026-06-17

## Current focus

**M1 complete.** Next: **M2** — chat CRUD + OpenAPI per [M-03-m2-chat-crud-openapi.md](../plans/M-03-m2-chat-crud-openapi.md).

## In progress

- [x] Project documentation
- [x] AI context architecture bootstrap
- [x] M1: `pom.xml`, Flyway V1, domain records, Modulith, tests
- [ ] M2: repositories, service, REST, springdoc, generated IT client

## Open questions

- Formal `REQ-###` ID table vs milestone-only traceability until M2+
- Upgrade Boot 3.4.4 → 4.1 / Modulith 2.1 timing (DEC-010)

## Active risks

| ID | Risk | Mitigation |
|---|---|---|
| RISK-001 | MCP hard dependency | M1–M6 without MCP |
| RISK-002 | Doc stack vs `pom.xml` | DEC-010; upgrade in M2–M3 |

## Next steps

1. TDD `ChatRepository` / `ChatService` (M2)
2. springdoc + OpenAPI Generator (DEC-007)
3. `ChatControllerIntegrationTest` with Testcontainers

## Mismatches (docs vs repo)

| Item | Doc says | Repo has |
|---|---|---|
| Spring Boot | 4.1.0 | 3.4.4 (DEC-010) |
| Spring Modulith | 2.1.0 | 1.3.4 (DEC-010) |
| Spring AI | 2.0.0 | Not in `pom.xml` yet (M3) |
