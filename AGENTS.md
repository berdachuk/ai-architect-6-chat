# AGENTS.md — ai-architect-6-chat (`ai-chat`)

General-purpose AI chat: multi-session history, SSE streaming, Harness progress, optional MCP enrichment. **v1.0.0** — M-17 OWASP hardening done; memory-bank migrated to multi-agent-safe layout (DEC-014). Next: [M-15 plan](.agents/plans/M-15-maintenance-backlog.md) · [activeContext](.agents/memory-bank/activeContext.md).

**Stack:** Java 21 · Spring Boot 4.1 · Spring AI 2.0 · Spring Modulith · PostgreSQL 17 · Thymeleaf SSR · Ollama (OpenAI-compatible client)

## Repo map

```text
src/main/java/com/berdachuk/aichat/
├── core/      shared config, security, OpenAiChatModelFactory, IdGenerator
├── chat/      sessions, messages, REST /api/v1/chats          → chat/AGENTS.md
├── llm/       orchestration, harness, advisors, SSE activity  → llm/AGENTS.md
├── mcp/       MCP client, McpServerRegistry (optional runtime) → mcp/AGENTS.md
├── web/       Thymeleaf SSR, chat.js                           → web/AGENTS.md
└── system/    actuator health (MCP indicator)
docs/          canonical SRS, SAD, SDD, testing, deployment
.agents/       skills, memory-bank, plans (agent context layer)
scripts/       sync-memory-index.sh (index regenerator + CI gate)
```

**Modulith deps:** `web→{core,chat,llm}` · `chat→core` · `llm→{core,chat,mcp}` · `mcp→core` · `system→{core,mcp :: registry}`

## Commands

| Action | Command |
|---|---|
| Unit + Modulith | `mvn test` |
| Integration | `mvn verify -Pintegration` |
| Run (local) | `mvn spring-boot:run` |
| Modulith verify | `ApplicationModules.of(AiChatApplication.class).verify()` |
| Regenerate memory indexes | `scripts/sync-memory-index.sh` |
| CI gate (assert in sync) | `scripts/sync-memory-index.sh --check` |

## Global boundaries

| | Rule |
|---|---|
| ✅ | JDBC only (`NamedParameterJdbcTemplate`); interfaces in `service/`/`repository/`, impl in `impl/` |
| ✅ | SQL in `sql/<module>/*.sql` via `@InjectSql`; named `:bind` only — no inline SQL (DEC-013) |
| ✅ | OpenAI-compatible `OpenAiChatModel`; default Ollama `http://localhost:11434/v1` |
| ✅ | Chat **must work without MCP** — degrade gracefully when MCP down (NFR-001) |
| ✅ | TDD: test first → requirement alignment → security pre-check → implement → verify |
| ✅ | Read memory bank + nearest module `AGENTS.md` at session start |
| ✅ | Memory-bank is multi-agent safe: append-only registries, one record per file (DEC-014) |
| ⚠️ | MCP enrichment phase 2 (M7–M8); do not block M1–M6 on MCP |
| ⚠️ | Update `.agents/memory-bank/` after meaningful doc/code changes |
| 🚫 | JPA/Hibernate, graph DB, medical domain logic, secrets in repo |
| 🚫 | `security-check` skill: no auto-fix vulns, no PR approval |
| 🚫 | Cross-module imports violating `@ApplicationModule` |
| 🚫 | Hand-edit generated index files — edit source registry/record + run `sync-memory-index.sh` |
| 🚫 | Edit an existing registry line — append only (breaks ID stability) |

## Module guidance

| Module | AGENTS.md | Owns |
|---|---|---|
| Chat persistence & API | [chat/AGENTS.md](src/main/java/com/berdachuk/aichat/chat/AGENTS.md) | `Chat`, `ChatMessage` |
| LLM & Harness | [llm/AGENTS.md](src/main/java/com/berdachuk/aichat/llm/AGENTS.md) | orchestration, session advisors, harness runs |
| MCP client | [mcp/AGENTS.md](src/main/java/com/berdachuk/aichat/mcp/AGENTS.md) | `McpServerRegistry`, tool wrappers |
| Web UI | [web/AGENTS.md](src/main/java/com/berdachuk/aichat/web/AGENTS.md) | Thymeleaf, static assets |

## Skills index (canonical: `.agents/skills/**/SKILL.md`)

| Skill | Load when |
|---|---|
| [core-architecture](.agents/skills/core-architecture/SKILL.md) | Modulith boundaries, package layout, new modules |
| [requirements-modeling](.agents/skills/requirements-modeling/SKILL.md) | SRS changes, REQ IDs, milestone mapping |
| [code-style](.agents/skills/code-style/SKILL.md) | Java/Spring JDBC patterns, naming |
| [testing](.agents/skills/testing/SKILL.md) | Unit/IT tests, Testcontainers, TDD workflow |
| [bdd-traceability](.agents/skills/bdd-traceability/SKILL.md) | Requirements → Gherkin → tests |
| [security-check](.agents/skills/security-check/SKILL.md) | Auth, APIs, DB, secrets, deps (pre + post impl) |
| [write-less-code](.agents/skills/write-less-code/SKILL.md) | Simplification pass before commit |

## Memory index

`.agents/memory-bank/` — read at session start; multi-agent safe (DEC-014).

**Reference files** (hand-edit; change rarely):

| File | Purpose |
|---|---|
| [projectbrief.md](.agents/memory-bank/projectbrief.md) | Identity, goals, scope |
| [systemPatterns.md](.agents/memory-bank/systemPatterns.md) | Modules, domain ownership, traceability gaps |
| [techContext.md](.agents/memory-bank/techContext.md) | Stack, commands, env |
| [productContext.md](.agents/memory-bank/productContext.md) | Capabilities, constraints (prose hand-edit; tables generated) |

**Generated index files** (do NOT hand-edit — edit source + run `sync-memory-index.sh`):

| File | Source |
|---|---|
| [activeContext.md](.agents/memory-bank/activeContext.md) | `records/active/M*.md` + `registry/risk.jsonl` + `registry/scn.jsonl` |
| [progress.md](.agents/memory-bank/progress.md) | `records/progress/M*.md` |
| [decisions.md](.agents/memory-bank/decisions.md) | `registry/dec.jsonl` |
| [plans/00-index.md](.agents/plans/00-index.md) | `records/{active,deferred,progress}/` + `plans/archive/` |

**Append-only registries** (`registry/*.jsonl`, one JSON object per line; mint `max+1`):

`req.jsonl` (REQ-###) · `nfr.jsonl` (NFR-###) · `scn.jsonl` (SCN-###) · `test.jsonl` (TEST-###) · `dec.jsonl` (DEC-###) · `risk.jsonl` (RISK-###) · `task.jsonl` (TASK-###) — schemas in [registry/SCHEMA.md](.agents/memory-bank/registry/SCHEMA.md).

**Per-record files** (`records/`): `progress/M{NN}.md`, `active/M{NN}.md`, `decisions/DEC-###.md`, `deferred/M{NN}.md` — one file per record → zero merge conflict between parallel agents.

**Module locks** (`locks/<module>.md`): acquire before editing coupled file pairs; see [locks/README.md](.agents/memory-bank/locks/README.md).

**Worktree scratchpads** (`worktrees/<branch-slug>/`, git-ignored): transient per-agent drafts — never merge to main.

Deep specs: [docs/README.md](docs/README.md) · Strategy: [docs/ai-context-strategy.md](docs/ai-context-strategy.md)

## Traceability (compact)

Stable IDs: `REQ-###` · `NFR-###` · `SCN-###` · `TEST-###` · `DEC-###` · `RISK-###` · `TASK-###` (in `registry/*.jsonl`). Milestones **M1–M17** in `records/progress/M{NN}.md`. When adding behavior:

1. Mint or reuse stable IDs in the matching `registry/*.jsonl` (append one line).
2. Map to owning module + domain models (`Chat`, `ChatMessage`, `McpServerInfo`, `HarnessWorkflowRun`).
3. For BDD: tag `@mN` / `@req-NNN`; link scenario → requirement in `registry/scn.jsonl`.
4. Prose traceability tables are generated in `productContext.md`; do not hand-edit.

## Session workflow

**Start:** reference memory files (4 above) → root `AGENTS.md` → nearest module `AGENTS.md` → skills per triggers.

**During:** TDD → requirement alignment → `security-check` (pre-impl for risky work) → implement → verify → `security-check` (post-impl). Respect Modulith boundaries + MCP optional invariant.

**End (multi-agent safe):**
1. Update `records/active/M{NN}.md` (or move to `records/progress/` on completion).
2. Append `registry/*.jsonl` rows for new `DEC/REQ/NFR/SCN/TEST/RISK/TASK` IDs — never edit existing lines.
3. Update `systemPatterns.md`/`techContext.md` if architecture/stack shifted.
4. Acquire/release `locks/<module>.md` for coupled-file edits.
5. Run `scripts/sync-memory-index.sh` to regenerate indexes (CI asserts `--check`).
6. Sync links to `docs/` if canonical docs changed.