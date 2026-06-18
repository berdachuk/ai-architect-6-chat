# M-17 — OWASP Security Hardening

**Status:** Draft  
**Date:** 2026-06-18

## Goal

Address two OWASP Top 10 risks applicable to the open-by-default architecture: A01 (Broken Access Control) and A10 (SSRF).

## Tasks

### 1. A01 — Validate X-User-Id header

- `UserContext.resolveDevUserId()`: validate header and cookie values against `[a-zA-Z0-9._\-@]+` pattern, max 100 chars
- Invalid values fall back to `anonymous` with a warning log
- Prevents XSS payload injection, overflow attacks, and arbitrary string pollution in DB

### 2. A10 — Validate MCP connection URLs

- `CreateMcpConnectionRequest`: add `@Pattern` validation on `url` field
- Only `http://` and `https://` schemes allowed
- Hostname must be `localhost`, `127.0.0.1`, `[::1]`, or a valid FQDN with TLD
- Blocks `file://`, internal IPs, bare hostnames, cloud metadata endpoints

## Verification

1. `curl -H "X-User-Id: <script>alert(1)</script>"` → falls back to `anonymous`
2. `curl -H "X-User-Id: $(python3 -c 'print("A"*10000)')"` → falls back to `anonymous`
3. `curl -X POST .../mcp/connections -d '{"url":"http://169.254.169.254/"}'` → HTTP 400
4. `curl -X POST .../mcp/connections -d '{"url":"file:///etc/passwd"}'` → HTTP 400
5. Valid IDs and URLs still work

## Deliverables

- `docs/security-assessment-owasp.md` — full risk assessment with before/after demonstrations
- `UserContext.java` — X-User-Id validation
- `CreateMcpConnectionRequest.java` — URL validation
