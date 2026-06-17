# Active context

**Updated:** 2026-06-17

## Current focus

**M2 complete.** Next: **M3** — LLM integration per [M-04-m3-llm-integration.md](../plans/M-04-m3-llm-integration.md).

## In progress

- [x] Project documentation
- [x] AI context architecture bootstrap
- [x] M1: `pom.xml`, Flyway V1, domain records, Modulith, tests
- [x] M2: repositories, service, REST, springdoc, generated IT client
- [ ] M3: Spring AI, `ChatAssistantService`, SSE streaming

## Open questions

- Formal `REQ-###` ID table vs milestone-only traceability until M3+

## Active risks

| ID | Risk | Mitigation |
|---|---|---|
| RISK-001 | MCP hard dependency | M1–M6 without MCP |
| RISK-002 | Docs ahead of code | Memory bank flags gaps; implement per milestone |

## Next steps

1. TDD `SpringAIConfig` + `OpenAiChatModelFactory` (M3)
2. `ChatAssistantService` SSE `token`/`done` endpoints
3. Session JDBC advisors (optional in M3 scope per plan)
