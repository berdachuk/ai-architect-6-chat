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
- **SQL:** `src/main/resources/sql/<module>/*.sql` — no inline SQL strings in Java
- **Config:** `OpenAiChatModelFactory` for all LLM endpoints; no OpenAI auto-config
- **Logging:** SLF4J; no secrets in logs
- Match existing module `AGENTS.md` boundaries

### Do

- Constructor injection
- `Optional` for single-row queries
- Package-private row mappers

### Don't

- JPA annotations
- `impl` types in REST constructors
- God services spanning modules

## Boundaries

- Do not change stack versions without updating `techContext.md` and `docs/`
- Do not add frameworks without ADR in `decisions.md`
