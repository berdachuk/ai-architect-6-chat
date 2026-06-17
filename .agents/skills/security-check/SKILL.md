---
name: security-check
description: Security review agent for AI-generated and human-written code
version: "1.0"
tags:
  - security
  - owasp
  - auth
  - secrets
  - dependencies
---

# Security Check

## Description

Security review for ai-chat: secrets hygiene, auth boundaries, input validation, dependency risks, OWASP-aligned quick review. For auth, APIs, DB, secrets, external input, infrastructure, new dependencies.

## When to use

- **Before implementation** on risky tasks (auth, REST, DB, MCP, config)
- **After implementation**, before commit
- Reviewing `SecurityConfig`, env vars, Docker compose

## Instructions

### Checklist

- **Secrets:** no keys/tokens in repo; `.env` gitignored; no secrets in memory bank
- **Auth:** local permit-all is dev-only; document prod JWT/OAuth gap
- **Input:** validate REST bodies; sanitize user message content before LLM
- **Injection:** parameterized JDBC only; no string-concat SQL
- **MCP:** validate tool inputs; treat MCP responses as untrusted
- **SSE:** rate limiting (future); timeout on `SseEmitter`
- **Dependencies:** review new Maven deps for known CVEs
- **OWASP:** injection, broken auth, sensitive data exposure, misconfiguration

### Blocking criteria

Escalate to human for **critical/high**: hardcoded secrets, SQL injection path, auth bypass, RCE via MCP/tool chain.

## Boundaries

- **Do not auto-fix vulnerabilities**
- **Do not approve PRs**
- Escalate critical issues to humans
- Report findings; implementation fixes are separate tasks
