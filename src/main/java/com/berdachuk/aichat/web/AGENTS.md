# AGENTS.md — `web` module

**Package:** `com.berdachuk.aichat.web`  
**Depends on:** `core`, `chat`, `llm`  
**Canonical design:** [docs/03-design.md](../../../../../../../docs/03-design.md) (ChatWebController, chat.html, chat.js)

## Purpose

Thymeleaf SSR UI and static assets. Renders chat page; delegates streaming to `llm` SSE endpoints. No domain persistence here.

## Layout

```text
web/
├── controller/     ChatWebController
resources/
├── templates/chat.html
└── static/css|js/  chat.css, chat.js (SSE client, Markdown via marked.js + DOMPurify)
```

## UI conventions

- Bootstrap 5.3; vanilla JS (no SPA framework)
- SSE client mirrors med-expert-match-ce event names
- Harness progress panel: collapsible activity stream

## Boundaries

| | |
|---|---|
| ✅ | SSR, static assets, form routing to APIs |
| 🚫 | JDBC, MCP client, advisor chain |
| 🚫 | Business rules in Thymeleaf or `chat.js` (keep in services) |

## Skills

- [code-style](../../../../../../../.agents/skills/code-style/SKILL.md)
- [write-less-code](../../../../../../../.agents/skills/write-less-code/SKILL.md) — avoid duplicating server logic in JS
