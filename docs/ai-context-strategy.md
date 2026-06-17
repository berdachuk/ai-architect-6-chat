# AI context strategy

**Version:** 1.0.0  
**Date:** 2026-06-17  
**Applies to:** ai-architect-6-chat (`ai-chat`)

## Purpose

Define how AI coding agents (Cursor, Claude Code, Copilot, etc.) consume project context in a tool-agnostic way.

## Layer model

```text
┌─────────────────────────────────────────────────────────┐
│  Optional IDE adapters (.cursor/, MCP, etc.) — future    │
├─────────────────────────────────────────────────────────┤
│  .agents/skills/<name>/SKILL.md   ← canonical skills     │
├─────────────────────────────────────────────────────────┤
│  .agents/memory-bank/*.md         ← session continuity   │
├─────────────────────────────────────────────────────────┤
│  Module AGENTS.md (2–5 files)     ← module boundaries    │
├─────────────────────────────────────────────────────────┤
│  AGENTS.md (root)                 ← compact index          │
├─────────────────────────────────────────────────────────┤
│  docs/*.md                        ← canonical deep specs │
└─────────────────────────────────────────────────────────┘
```

**Rule:** `docs/` holds authoritative technical detail. Memory bank holds distilled operational state. Skills hold reusable workflows. Root `AGENTS.md` links — does not duplicate.

## Module analysis → context mapping

Discovered/planned architecture (from [02-architecture.md](02-architecture.md)):

| Module | AGENTS.md path | Skills most used |
|---|---|---|
| `chat` | `src/main/java/com/berdachuk/aichat/chat/AGENTS.md` | code-style, testing |
| `llm` | `src/main/java/com/berdachuk/aichat/llm/AGENTS.md` | core-architecture, security-check |
| `mcp` | `src/main/java/com/berdachuk/aichat/mcp/AGENTS.md` | core-architecture, testing |
| `web` | `src/main/java/com/berdachuk/aichat/web/AGENTS.md` | code-style, write-less-code |
| `core`, `system` | root AGENTS.md only | core-architecture |

Domain ownership:

- `Chat`, `ChatMessage` → `chat`
- Harness runtime → `llm`
- `McpServerInfo` → `mcp` (in-memory)

## Session workflow

### Start (substantial task)

1. `.agents/memory-bank/projectbrief.md`
2. `.agents/memory-bank/activeContext.md`
3. `.agents/memory-bank/systemPatterns.md`
4. `.agents/memory-bank/techContext.md`
5. Root `AGENTS.md`
6. Nearest module `AGENTS.md`
7. Load skills per triggers in root AGENTS.md

### During

- TDD + requirement alignment + security-check (see root AGENTS.md)
- Respect Modulith boundaries
- MCP optional invariant

### End

- Update `activeContext.md`, append `progress.md`
- `decisions.md` if architecture changed
- Sync links if `docs/` edited

## Traceability

| ID type | Usage |
|---|---|
| M1–M9 | Implementation milestones (current) |
| REQ-### | Introduce when coding (requirements-modeling skill) |
| SCN-### | BDD scenarios (bdd-traceability skill) |
| DEC-### | `memory-bank/decisions.md` |
| RISK-### | `activeContext.md` |

Prefer tables and stable links over prose-only traceability.

## Adding skills

1. Create `.agents/skills/<kebab-name>/SKILL.md`
2. YAML frontmatter: `name`, `description`, `version`, `tags`
3. Sections: Description, When to use, Instructions, Boundaries
4. Add row to root AGENTS.md Skills index
5. Do **not** duplicate into IDE-specific folders — adapters transform from here

## Maintaining memory bank

| Event | Update |
|---|---|
| Task focus change | `activeContext.md` |
| Completed work | `progress.md` (dated) |
| Architecture change | `systemPatterns.md`, `decisions.md` |
| Stack/tooling change | `techContext.md` |
| Doc/code mismatch | Note in `activeContext.md` |

**Never store:** secrets, large code dumps, raw chat logs.

## IDE adapters (future)

- Cursor: may symlink or generate rules from `AGENTS.md` + skills
- MCP: optional project tools reading `docs/` and memory bank
- Canonical source remains `.agents/skills` and `.agents/memory-bank`

## Java Cucumber rule

When Cucumber is adopted:

- `.feature` files in acceptance-test layer
- Tags: `@req-NNN`, `@mN`, module tags (`@chat`, `@llm`)
- Thin step definitions; domain logic in services
- Scenario names = business outcomes

## Anti-patterns

- REQ IDs in docs but not in tests
- Scenarios mirroring screens not behavior
- Nested AGENTS.md duplicating root content
- Memory bank copying full SRS
- Stale traceability after refactor

## Related

- [AGENTS.md](../AGENTS.md)
- [.agents/plans/00-index.md](../.agents/plans/00-index.md)
- [docs/README.md](README.md)
