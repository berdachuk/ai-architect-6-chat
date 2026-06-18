# Security Assessment — OWASP Top 10 Risks

**Date:** 2026-06-18  
**Application:** `ai-chat` v1.0.0  
**Assessment scope:** Two OWASP Top 10 risks relevant to the application's open-by-default architecture.

---

## Risk 1: A01 — Broken Access Control (User Impersonation via `X-User-Id`)

### Vulnerability demonstration

**Vulnerable code** (`UserContext.resolveDevUserId()`):

```java
private String resolveDevUserId() {
    String headerId = request.getHeader("X-User-Id");
    if (headerId != null && !headerId.isBlank()) {
        return headerId;  // ← no validation
    }
    ...
}
```

**Attack:**

```bash
# Impersonate another user by setting any X-User-Id header
curl -H "X-User-Id: admin" http://localhost:8095/api/v1/chats
# Returns admin's chat list — attacker sees all their sessions

# Inject malicious payloads via header
curl -H "X-User-Id: <script>alert('xss')</script>" \
  http://localhost:8095/api/v1/chats
# Stored in DB, rendered in Thymeleaf without escaping in some contexts

# Overflow attack
curl -H "X-User-Id: $(python3 -c 'print("A"*10000)')" \
  http://localhost:8095/api/v1/chats
# 10KB string stored as user_id in every chat/message row
```

### Risk assessment (before mitigation)

| Factor | Rating |
|--------|--------|
| Likelihood | High — single HTTP header, no authentication required |
| Impact | Medium — data isolation broken, DB pollution, potential XSS |
| Severity | **High** |
| Exploitability | Trivial — `curl -H "X-User-Id: target"` |

### Mitigation

**Updated code** (`UserContext.resolveDevUserId()`):

```java
private static final int MAX_USER_ID_LENGTH = 100;
private static final String USER_ID_PATTERN = "[a-zA-Z0-9._\\-@]+";

private String resolveDevUserId() {
    String headerId = request.getHeader("X-User-Id");
    if (headerId != null && !headerId.isBlank()) {
        if (headerId.length() > MAX_USER_ID_LENGTH
                || !headerId.matches(USER_ID_PATTERN)) {
            log.warn("Rejected invalid X-User-Id header");
            return "anonymous";
        }
        return headerId;
    }
    // Same validation for cookie value
    ...
}
```

**Failed attacks after mitigation:**

```bash
# XSS payload → rejected, falls back to "anonymous"
curl -H "X-User-Id: <script>alert(1)</script>" \
  http://localhost:8095/api/v1/chats
# Returns anonymous's chats, not attacker-controlled

# Overflow → rejected
curl -H "X-User-Id: $(python3 -c 'print("A"*10000)')" \
  http://localhost:8095/api/v1/chats
# Returns anonymous's chats

# Valid IDs still work
curl -H "X-User-Id: test-user" http://localhost:8095/api/v1/chats
# Returns test-user's chats ✓
```

### Risk assessment (after mitigation)

| Factor | Rating |
|--------|--------|
| Likelihood | Low — only alphanumeric + `.` `_` `-` `@` accepted, max 100 chars |
| Impact | Low — invalid headers fall back to `anonymous`, no DB pollution |
| Severity | **Low** |
| Residual risk | User impersonation still possible with valid-format IDs (e.g. `admin`). Full mitigation requires OAuth2/JWT authentication. |

---

## Risk 2: A10 — Server-Side Request Forgery (SSRF via MCP Connection URL)

### Vulnerability demonstration

**Vulnerable code** (`CreateMcpConnectionRequest`):

```java
public record CreateMcpConnectionRequest(
        @NotBlank String name,
        @NotBlank String url,  // ← any URL accepted
        boolean tools,
        boolean resources,
        boolean prompts) {
}
```

**Attack:**

```bash
# Probe internal network
curl -X POST http://localhost:8095/api/v1/mcp/connections \
  -H "Content-Type: application/json" \
  -H "X-User-Id: attacker" \
  -d '{"name":"internal","url":"http://169.254.169.254/latest/meta-data/","tools":true}'
# If on AWS, McpClientConnector.connect() would fetch instance metadata

# Scan internal ports
curl -X POST http://localhost:8095/api/v1/mcp/connections \
  -H "Content-Type: application/json" \
  -H "X-User-Id: attacker" \
  -d '{"name":"db-probe","url":"http://postgres:5432/sse","tools":true}'
# Attempts to connect to internal DB host

# File protocol attack
curl -X POST http://localhost:8095/api/v1/mcp/connections \
  -H "Content-Type: application/json" \
  -H "X-User-Id: attacker" \
  -d '{"name":"local","url":"file:///etc/passwd","tools":true}'
# Would attempt file:// protocol connection
```

### Risk assessment (before mitigation)

| Factor | Rating |
|--------|--------|
| Likelihood | High — unauthenticated REST endpoint, any URL accepted |
| Impact | High — internal network scanning, cloud metadata access, file reads |
| Severity | **Critical** |
| Exploitability | Trivial — `POST /api/v1/mcp/connections` with arbitrary URL |

### Mitigation

**Updated code** (`CreateMcpConnectionRequest`):

```java
public record CreateMcpConnectionRequest(
        @NotBlank String name,
        @NotBlank
        @Pattern(regexp = "https?://(localhost|127\\.0\\.0\\.1|\\[::1\\]|[a-zA-Z0-9][-a-zA-Z0-9.]*\\.[a-zA-Z]{2,})(:\\d{1,5})?(/[-a-zA-Z0-9._~:/?#\\[\\]@!$&'()*+,;=%]*)?",
                message = "URL must use http/https scheme with a valid public or localhost hostname")
        String url,
        boolean tools,
        boolean resources,
        boolean prompts) {
}
```

**Failed attacks after mitigation:**

```bash
# Cloud metadata → rejected (IP address not in allowlist)
curl -X POST http://localhost:8095/api/v1/mcp/connections \
  -H "Content-Type: application/json" \
  -H "X-User-Id: attacker" \
  -d '{"name":"aws","url":"http://169.254.169.254/latest/meta-data/","tools":true}'
# HTTP 400: "URL must use http/https scheme with a valid public or localhost hostname"

# Internal host → rejected (no TLD)
curl -X POST http://localhost:8095/api/v1/mcp/connections \
  -H "Content-Type: application/json" \
  -H "X-User-Id: attacker" \
  -d '{"name":"db","url":"http://postgres:5432/sse","tools":true}'
# HTTP 400: rejected

# File protocol → rejected (scheme not http/https)
curl -X POST http://localhost:8095/api/v1/mcp/connections \
  -H "Content-Type: application/json" \
  -H "X-User-Id: attacker" \
  -d '{"name":"file","url":"file:///etc/passwd","tools":true}'
# HTTP 400: rejected

# Valid URL still works
curl -X POST http://localhost:8095/api/v1/mcp/connections \
  -H "Content-Type: application/json" \
  -H "X-User-Id: test-user" \
  -d '{"name":"test","url":"http://localhost:8092/sse","tools":true}'
# HTTP 201: created ✓
```

### Risk assessment (after mitigation)

| Factor | Rating |
|--------|--------|
| Likelihood | Low — only http/https to valid hostnames with TLD or localhost |
| Impact | Low — internal IPs, bare hostnames, file:// blocked |
| Severity | **Low** |
| Residual risk | Attacker can still point to external malicious MCP servers. Full mitigation requires authentication on the POST endpoint + URL allowlist. |

---

## Summary

| Risk | Before | After | Residual |
|------|--------|-------|----------|
| A01 — Broken Access Control | **High** | **Low** | User impersonation with valid-format IDs still possible |
| A10 — SSRF | **Critical** | **Low** | External malicious MCP servers still connectable |

### Files changed

| File | Change |
|------|--------|
| `UserContext.java` | Validate `X-User-Id` header and cookie: max 100 chars, alphanumeric + `.` `_` `-` `@` only |
| `CreateMcpConnectionRequest.java` | `@Pattern` validation: http/https only, valid hostname with TLD or localhost |
