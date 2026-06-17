# Active context

**Updated:** 2026-06-17

## Current focus

Package namespace set to **`com.berdachuk.aichat`**. Bootstrap AI context layer complete. **Next:** milestone **M1** (schema + Modulith foundation).

## In progress

- [x] Project documentation (SRS, SAD, SDD, testing, deployment)
- [x] AI context architecture bootstrap
- [ ] `pom.xml` and `AiChatApplication` stub (M1)

## Open questions

- Formal `REQ-###` ID table vs milestone-only traceability until M2+
- Cucumber adoption timing (docs mention IT; no features yet)

## Active risks

| ID | Risk | Mitigation |
|---|---|---|
| RISK-001 | MCP treated as hard dependency | Documented invariant; M1–M6 without MCP |
| RISK-002 | Docs ahead of code | Memory bank flags gaps; implement per milestone |

## Next steps

1. M1: `pom.xml`, Flyway `V1`, domain records, `package-info.java`, `ModulithArchitectureTest`
2. Assign `REQ-###` IDs when starting M2 (requirements-modeling skill)
3. TDD + security-check on first REST endpoint

## Mismatches (docs vs repo)

| Item | Doc says | Repo has |
|---|---|---|
| Full `src/` tree | [docs/02-architecture.md](../../docs/02-architecture.md) | Only module `AGENTS.md` stubs |
| `pom.xml` | Planned | Missing |
