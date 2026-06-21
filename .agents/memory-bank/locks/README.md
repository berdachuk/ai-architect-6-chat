# Module locks

Module locks make **silent semantic conflicts explicit**. Before editing a coupled file pair
(two files that must change in lockstep), an agent creates or overwrites `locks/<module>.md`.
If a non-expired lock already exists for a different branch, the second agent must wait or pick a
different module.

Locks are **advisory but enforced by the `code-style` and `security-check` skills**. CI does not
block on locks, but a merged lock file for a different branch is a review red flag.

## Lock file format

`locks/<module>.md`:

```markdown
# Lock — <module>

- held-by: <branch-slug>
- acquired: YYYY-MM-DDThh:mm:ssZZ
- expires: YYYY-MM-DDThh:mm:ssZZ   # 24h max; stale locks are ignorable
- coupled files:
  - path/to/file.a
  - path/to/file.b
- reason: <one line>
```

## Acquisition rules

1. Before editing any coupled file pair, read `locks/<module>.md` (if it exists).
2. If it exists and `held-by` is a different branch and `expires` is in the future → stop. Pick a
   different module or coordinate with the other agent.
3. If it does not exist, or is expired, or is held by your own branch → create/overwrite it with
   your branch + a 24h expiry.
4. On merge (or when done), release the lock: delete the file or set `held-by: released`.

## Coupled-file-pair table (known lockstep risks)

| Module | File A | File B | Why coupled |
|---|---|---|---|
| `mcp` | `McpServerInfo` (record) | `McpServerRegistry` catalog text | Field added to record must surface in catalog |
| `llm` | `LlmChatClientConfiguration` | `ChatWorkflowEngine` | Advisor wiring ↔ harness stage SSE contract |
| `web` | `chat.html` | `chat.js` | DOM element IDs ↔ JS selectors |
| `core` | `application.yml` | `AiChatProperties` | Bind key ↔ `@ConfigurationProperties` field |
| `core` | `UserContext` | `SecurityConfig` | Principal extraction ↔ authorized endpoint rules |
| `chat` | Flyway `V*__*.sql` | repository `@InjectSql` SQL files | Schema column ↔ bind name |

When you discover a new coupled pair, append a row here (append-only).

## What locks are NOT

- Not a distributed lock service — they are text files resolved at git merge time.
- Not a substitute for tests — they make the *need* for coordination visible, not the correctness.
- Not for single-file edits — only for lockstep pairs.