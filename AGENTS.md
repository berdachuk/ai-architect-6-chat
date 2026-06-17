# AGENTS.md — ai-architect-6-chat (`ai-chat`)

General-purpose AI chat: multi-session history, SSE streaming, Harness progress, optional MCP enrichment. **Docs-only repo today** — implementation planned per [docs/01-requirements.md](docs/01-requirements.md) milestones M1–M9.

**Stack:** Java 21 · Spring Boot 4.1 · Spring AI 2.0 · Spring Modulith · PostgreSQL 17 · Thymeleaf SSR · Ollama (OpenAI-compatible client)

## Repo map (planned)

```text
src/main/java/com/berdachuk/aichat/
├── core/      shared config, security, OpenAiChatModelFactory, IdGenerator
├── chat/      sessions, messages, REST /api/v1/chats          → chat/AGENTS.md
├── llm/       orchestration, harness, advisors, SSE activity  → llm/AGENTS.md
├── mcp/       MCP client, McpServerRegistry (optional runtime)  → mcp/AGENTS.md
├── web/       Thymeleaf SSR, chat.js                           → web/AGENTS.md
└── system/    actuator health (MCP indicator)
docs/          canonical SRS, SAD, SDD, testing, deployment
.agents/       skills, memory-bank, plans (agent context layer)
```

**Modulith deps:** `web→{core,chat,llm}` · `chat→core` · `llm→{core,chat,mcp}` · `mcp→core` · `system→{core,mcp}`

## Commands (once `pom.xml` exists)

| Action | Command |
|---|---|
| Unit + Modulith | `mvn test` |
| Integration | `mvn verify -Pintegration` |
| Run (local) | `mvn spring-boot:run` |
| Modulith verify | `ApplicationModules.of(AiChatApplication.class).verify()` |

## Global boundaries

| | Rule |
|---|---|
| ✅ | JDBC only (`NamedParameterJdbcTemplate`); interfaces in `service/`/`repository/`, impl in `impl/` |
| ✅ | OpenAI-compatible `OpenAiChatModel`; default Ollama `http://localhost:11434/v1` |
| ✅ | Chat **must work without MCP** — degrade gracefully when MCP down |
| ✅ | TDD: test first → requirement alignment → security pre-check → implement → verify |
| ✅ | Read memory bank + nearest module `AGENTS.md` at session start |
| ⚠️ | MCP enrichment phase 2 (M7–M8); do not block M1–M6 on MCP |
| ⚠️ | Update `.agents/memory-bank/` after meaningful doc/code changes |
| 🚫 | JPA/Hibernate, graph DB, medical domain logic, secrets in repo |
| 🚫 | `security-check` skill: no auto-fix vulns, no PR approval |
| 🚫 | Cross-module imports violating `@ApplicationModule` |

## Module guidance

| Module | AGENTS.md | Owns |
|---|---|---|
| Chat persistence & API | [src/main/java/com/berdachuk/aichat/chat/AGENTS.md](src/main/java/com/berdachuk/aichat/chat/AGENTS.md) | `Chat`, `ChatMessage` |
| LLM & Harness | [src/main/java/com/berdachuk/aichat/llm/AGENTS.md](src/main/java/com/berdachuk/aichat/llm/AGENTS.md) | orchestration, session advisors, harness runs |
| MCP client | [src/main/java/com/berdachuk/aichat/mcp/AGENTS.md](src/main/java/com/berdachuk/aichat/mcp/AGENTS.md) | `McpServerRegistry`, tool wrappers |
| Web UI | [src/main/java/com/berdachuk/aichat/web/AGENTS.md](src/main/java/com/berdachuk/aichat/web/AGENTS.md) | Thymeleaf, static assets |

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

## Memory index (read at session start)

| File | Purpose |
|---|---|
| [projectbrief.md](.agents/memory-bank/projectbrief.md) | Identity, goals, scope |
| [activeContext.md](.agents/memory-bank/activeContext.md) | Current focus, next steps |
| [systemPatterns.md](.agents/memory-bank/systemPatterns.md) | Modules, domain ownership |
| [techContext.md](.agents/memory-bank/techContext.md) | Stack, commands, env |
| [productContext.md](.agents/memory-bank/productContext.md) | User-facing capabilities |
| [progress.md](.agents/memory-bank/progress.md) | Completed work log |
| [decisions.md](.agents/memory-bank/decisions.md) | ADR-style decisions |

Deep specs: [docs/README.md](docs/README.md) · Strategy: [docs/ai-context-strategy.md](docs/ai-context-strategy.md)

## Traceability (compact)

Use stable IDs from [docs/01-requirements.md](docs/01-requirements.md): milestones **M1–M9**, goals in §2. When adding behavior:

1. Link to milestone / section (provisional `REQ-###` until formal ID table exists).
2. Map to owning module + domain models (`Chat`, `ChatMessage`, `McpServerInfo`, `HarnessWorkflowRun`).
3. For BDD: tag `@mN` or `@req-NNN`; record in memory bank.

## Session workflow

**Start:** memory bank (4 files above) → root `AGENTS.md` → module `AGENTS.md` → relevant skills.

**End:** update `activeContext.md`, append `progress.md`, `decisions.md` if needed; sync links to `docs/`.
