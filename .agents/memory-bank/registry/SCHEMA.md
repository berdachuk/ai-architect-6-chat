# Registry schemas

Each file in this directory is **JSONL** (one JSON object per line, UTF-8, no trailing comma).
Append-only: **never edit an existing line** — it breaks ID stability and forces manual merge
resolution. To mint an ID, read the file, take `max+1`, append exactly one line. On a last-line
merge conflict, re-read and recompute.

Generated index files (`../activeContext.md`, `../progress.md`, `../decisions.md`,
`../productContext.md` traceability tables, `../../plans/00-index.md`) are regenerated from these
registries + `../records/**` by `scripts/sync-memory-index.sh`. Never hand-edit them.

## Files

| File | ID kind | Example ID |
|---|---|---|
| `req.jsonl`  | functional requirement       | `REQ-001` |
| `nfr.jsonl`  | non-functional requirement    | `NFR-001` |
| `scn.jsonl`  | executable behavior scenario  | `SCN-001` |
| `test.jsonl`| test artifact                | `TEST-001` |
| `dec.jsonl`  | architecture/process decision| `DEC-014` |
| `risk.jsonl` | known risk                   | `RISK-001` |
| `task.jsonl` | plan task                    | `TASK-001` |

## Common fields (every row)

```json
{"id":"DEC-014","status":"Accepted","date":"2026-06-21","title":"…","summary":"…"}
```

| Field   | Type   | Notes |
|---------|--------|-------|
| `id`    | string | Stable, zero-padded, kind-prefixed. Never reused, never edited. |
| `status`| string | `Accepted`, `Proposed`, `Superseded`, `Deprecated`, `Open`, `Mitigated`, `Done`, `Done(fail)`. |
| `date`  | string | ISO date `YYYY-MM-DD` of minting/last status change. |
| `title` | string | Short, unique human label. |
| `summary` | string | One-sentence rationale. |

## Per-kind extra fields

- `req.jsonl`  → `module`, `domain_models` (array), `milestone`, `scn` (array), `test` (array), `impl` (array of repo-relative paths).
- `nfr.jsonl`  → `category` (perf/security/...), `target`, `module`.
- `scn.jsonl`  → `req` (array), `module`, `feature_file`, `steps` (array of `STEP-###`).
- `test.jsonl` → `class`, `method`, `kind` (unit/integration/e2e), `scn` (array), `module`.
- `dec.jsonl`  → `module` (array), `superseded_by` (string or null), `body` (path to `../records/decisions/DEC-###.md`).
- `risk.jsonl` → `module`, `severity` (critical/high/medium/low), `mitigation`, `status`.
- `task.jsonl` → `milestone`, `plan` (path), `status`.

## Legacy aliases

Pre-migration decisions were minted as `DEC-001`..`DEC-013` inside a single `decisions.md` file.
They have been migrated to `dec.jsonl` + `records/decisions/DEC-00N.md`. Legacy `D-###` (if any)
are immutable aliases — do not rewrite.

## Conflict resolution

When two agents append concurrently and Git reports a conflict on the last line(s):

1. Keep both lines (they are distinct JSON objects).
2. If both minted the same `max+1` ID, the second writer bumps to `max+2` and updates its
   `records/**` reference.
3. Re-run `scripts/sync-memory-index.sh` to regenerate indexes.

This keeps conflicts textual and trivial — never edit an existing line to "fix" one.