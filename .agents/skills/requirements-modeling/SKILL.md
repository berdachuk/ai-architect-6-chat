---
name: requirements-modeling
description: Normalize requirements, milestone mapping, and stable IDs for ai-chat
version: "1.0"
tags:
  - requirements
  - traceability
  - srs
---

# Requirements Modeling

## Description

Maintains alignment between [docs/01-requirements.md](../../docs/01-requirements.md), milestones M1–M9, and implementation artifacts. Introduces stable `REQ-###` IDs when behavior is implemented.

## When to use

- Adding or changing functional requirements
- Starting a new milestone (M1–M9)
- Mapping tests to requirements
- Resolving ambiguity in SRS vs design

## Instructions

- Canonical SRS: `docs/01-requirements.md`
- Current IDs: milestones **M1–M9** (§14); promote to `REQ-###` when coding starts
- Table format for new requirements:

| Requirement ID | Summary | Module | Domain models | Milestone |
|---|---|---|---|---|

- Preserve invariants: MCP optional (§5), Ollama default (§7), SSE contract (§6)
- Non-goals in §2 are hard boundaries — do not implement
- Update `activeContext.md` when requirements ambiguous
- Cross-link BDD scenarios via `bdd-traceability` skill

## Boundaries

- Do not duplicate full SRS in memory bank — summarize + link
- Do not invent requirements without doc or human approval
- Do not rename milestones without updating plans index
