---
name: core-architecture
description: Spring Modulith structure, module boundaries, and dependency rules for ai-chat
version: "1.0"
tags:
  - architecture
  - modulith
  - modules
---

# Core Architecture

## Description

Guides package module layout, `@ApplicationModule` dependencies, and domain ownership for `com.berdachuk.aichat`. Covers `core`, `chat`, `llm`, `mcp`, `web`, `system`.

## When to use

- Adding a new package or cross-module dependency
- Refactoring boundaries between chat, llm, mcp, web
- Reviewing `ModulithArchitectureTest` failures
- Placing new classes (service vs impl vs rest)

## Instructions

- Follow dependency graph in [docs/02-architecture.md](../../docs/02-architecture.md) and [systemPatterns.md](../memory-bank/systemPatterns.md)
- `core` has no module dependencies
- `chat` owns `Chat`, `ChatMessage` — no LLM in `chat/`
- `llm` orchestrates; calls `ChatService`, not repositories directly
- `mcp` is client-only; no JDBC
- Interfaces in `service/` / `repository/`; `@Service`/`@Repository` only on `impl/`
- Run `ApplicationModules.verify()` after structural changes
- Link changes to milestone (M1–M9) in memory bank

## Boundaries

- Do not introduce JPA, graph DB, or medical domain packages
- Do not let `web` or `mcp` depend on `llm` impl types
- Do not change Modulith rules without updating `decisions.md`
