# Release notes

## v1.0.0 (2026-06-17)

First stable release of **ai-chat** — general-purpose AI chat with optional MCP enrichment.

### Highlights

- Multi-session chat with SSE streaming and Markdown rendering
- Spring AI Session JDBC memory (turn-window compaction)
- Harness workflow engine with agent progress UI
- MCP runtime catalog and per-chat tool toggles
- Docker Compose packaging and GitHub Actions CI
- Playwright browser E2E + REST smoke tests
- Optional OAuth2/JWT (`oauth2` profile) and browser OIDC login (`oauth2-login` profile)
- Prometheus metrics (`prod` profile) and Grafana dashboard JSON
- User guide: [docs/user-guide.md](docs/user-guide.md)

### Requirements delivered

Milestones M1–M9 per [docs/01-requirements.md](docs/01-requirements.md).

### Upgrade / run

```bash
git checkout v1.0.0
docker compose up --build
```

Default security is **open** (`ai-chat.security.oauth2-enabled: false`) for local development and testing.
