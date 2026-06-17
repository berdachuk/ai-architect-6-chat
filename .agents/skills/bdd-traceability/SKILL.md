---
name: bdd-traceability
description: Link requirements, Gherkin scenarios, tests, and implementation for ai-chat
version: "1.0"
tags:
  - bdd
  - gherkin
  - traceability
---

# BDD Traceability

## Description

Preserve explicit links between functional requirements, domain language, Gherkin scenarios, step definitions, and implementation artifacts.

## When to use

- New or changed functional requirements
- New or updated Gherkin feature files
- Review of acceptance coverage
- TDD tasks with executable business specifications
- Refactoring that may break requirement-to-test links

## Instructions

1. Extract behavior in business language first
2. Assign or reuse stable ID (`REQ-###` or milestone `M#`)
3. Identify owning module and domain models (`Chat`, `ChatMessage`, etc.)
4. Derive minimal Gherkin feature; scenarios as `SCN-###`
5. One business outcome per scenario
6. Use domain vocabulary from [docs/01-requirements.md](../../docs/01-requirements.md) — not UI mechanics
7. Tag scenarios: `@req-001` `@chat` `@m2`
8. Map scenarios → step defs → `TEST-###` / test class
9. Record links in plan or `activeContext.md`
10. When behavior changes, update mappings **with** code

### Example

```gherkin
@m2 @req-chat-create
Feature: Chat sessions

  Scenario: SCN-001 User creates a new chat session
    Given the user has no existing chats
    When the user creates a chat named "Research"
    Then a chat exists with name "Research" and is not the default
```

## Boundaries

- Do not invent business rules unsupported by docs or human instruction
- Do not merge unrelated requirements into one scenario
- Do not treat Gherkin as authoritative when code and approved docs contradict
- Do not mark traceability complete without verifying links
