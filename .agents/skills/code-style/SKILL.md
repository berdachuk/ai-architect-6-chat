---
name: code-style
description: Java 21 and Spring JDBC idioms for ai-chat
version: "1.0"
tags:
  - java
  - spring
  - jdbc
---

# Code Style

## Description

Naming, patterns, and idioms for `com.berdachuk.aichat` — aligned with med-expert-match-ce.

## When to use

- Writing Java in any module
- Adding repositories, services, controllers
- SQL externalization

## Instructions

- **Records** for domain (`Chat`, `ChatMessage`)
- **Repository:** interface + `impl/` with `NamedParameterJdbcTemplate`, `@InjectSql`
- **Service:** interface + `impl/` with `@Transactional` where needed
- **IDs:** `IdGenerator.generateId()` — 24-char hex
- **SQL:** `src/main/resources/sql/<module>/*.sql` — **no inline SQL** in Java (DEC-013)
- **Bind variables:** `:name` in SQL files only; use `NamedParameterJdbcTemplate` + `MapSqlParameterSource` / `Map.of` — never positional `?`
- **Inject:** `@InjectSql("/sql/<module>/file.sql")` on `private String` fields in repository `impl` (see [MedicalCaseRepositoryImpl](https://github.com/berdachuk/ai-architect-6-mcp/blob/main/src/main/java/com/example/medicalmcp/medicalcase/repository/impl/MedicalCaseRepositoryImpl.java))
- **Config:** `OpenAiChatModelFactory` for all LLM endpoints; no OpenAI auto-config
- **Logging:** SLF4J; no secrets in logs
- Match existing module `AGENTS.md` boundaries

### Do

- Constructor injection
- `Optional` for single-row queries
- Package-private row mappers

### Don't

- JPA annotations
- Inline SQL strings or `?` positional JDBC in repository code (DEC-013)
- `JdbcTemplate` for application queries (use `NamedParameterJdbcTemplate`)
- `impl` types in REST constructors
- God services spanning modules

## Boundaries

- Do not change stack versions without updating `techContext.md` and `docs/`
- Do not add frameworks without ADR in `decisions.md`
