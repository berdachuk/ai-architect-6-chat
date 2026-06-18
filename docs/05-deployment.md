# Deployment
## Operations & deployment guide

**Version:** 1.0.0
**Requirements:** [01-requirements.md](01-requirements.md) · [02-architecture.md](02-architecture.md)

---

## Application Configuration

### `application.yml` (canonical — matches `src/main/resources/application.yml`)

```yaml
spring:
  datasource:
    url: jdbc:postgresql://${AICHAT_DB_HOST:localhost}:${AICHAT_DB_PORT:5437}/${AICHAT_DB_NAME:ai_chat}?currentSchema=ai_chat
    username: ${AICHAT_DB_USERNAME:ai_chat}
    password: ${AICHAT_DB_PASSWORD:ai_chat}
  ai:
    mcp:
      client:
        enabled: false   # runtime catalog via REST + McpBootstrapSeeder
    custom:
      chat:
        provider: ollama
        base-url: ${AICHAT_CHAT_BASE_URL:http://localhost:11434}
        api-key: ${AICHAT_CHAT_API_KEY:ollama}
        model: ${AICHAT_CHAT_MODEL:gemma4:31b-cloud}
      chat-alt:
        base-url: ${AICHAT_CHAT_BASE_URL:http://localhost:11434}
        model: ${AICHAT_CHAT_ALT_MODEL:gemma4:31b-cloud}
      tool-calling:
        base-url: ${AICHAT_CHAT_BASE_URL:http://localhost:11434}
        model: ${AICHAT_TOOL_MODEL:gemma4:31b-cloud}

server:
  port: ${SERVER_PORT:8095}

management:
  endpoints:
    web:
      exposure:
        include: health,info
  endpoint:
    health:
      show-details: never        # use test profile or MANAGEMENT_* override for details
      show-components: never

ai-chat:
  features:
    mcp-client: true
  mcp:
    bootstrap:
      enabled: true
      name: medical-dataset
      url: ${MCP_MEDICAL_URL:http://localhost:8092/sse}
```

**Profiles:**

| Profile | Purpose |
|---|---|
| `test` | Stub LLM + health details for integration tests (`application-test.yml`) |
| `prod` | Stricter actuator defaults (`application-prod.yml`) |

Set `SPRING_PROFILES_ACTIVE=prod` in production. Override health detail exposure with `MANAGEMENT_ENDPOINT_HEALTH_SHOW_DETAILS=when_authorized` if needed.

**Metrics:** with `prod` profile, scrape `GET /actuator/prometheus` (requires `micrometer-registry-prometheus`).

**Grafana:** import [observability/grafana/ai-chat-overview.json](../observability/grafana/ai-chat-overview.json) and point at your Prometheus datasource. Scrape target example:

```yaml
scrape_configs:
  - job_name: ai-chat
    metrics_path: /actuator/prometheus
    static_configs:
      - targets: ['localhost:8095']
```

Run with `SPRING_PROFILES_ACTIVE=prod` so the Prometheus endpoint is exposed.

**Alerting:** example rules in [observability/prometheus/alerts.yml](../observability/prometheus/alerts.yml).

**Browser OIDC login:** `SPRING_PROFILES_ACTIVE=oauth2-login` with `OAUTH2_CLIENT_ID`, `OAUTH2_CLIENT_SECRET`, `OAUTH2_ISSUER_URI` — see `application-oauth2-login.yml`. Web UI omits `X-User-Id` when login is enabled.

### User identity and OAuth2 (optional)

**Default (no OAuth2):** `ai-chat.security.oauth2-enabled: false` — all endpoints are open. User id comes from the `X-User-Id` header or `aichat-user-id` cookie (fallback `anonymous`). Use this for local dev and automated tests.

**OAuth2/JWT (opt-in):** activate profile `oauth2` and set your IdP issuer:

```bash
export OAUTH2_ISSUER_URI=https://your-idp.example.com/realms/your-realm
export SPRING_PROFILES_ACTIVE=oauth2
```

With `oauth2` profile:

| Area | Behavior |
|---|---|
| `/api/v1/**` | Requires valid `Authorization: Bearer <jwt>` |
| Web UI (`/`, `/chat/**`) | Open; identity from JWT if present, else `X-User-Id` / cookie |
| User id claim | `ai-chat.security.jwt-user-claim` (default `sub`) |

See `application-oauth2.yml` for the profile template.

### Security filter (dev default)

```java
@Bean
SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(s -> s.sessionCreationPolicy(STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/", "/chat/**", "/css/**", "/js/**",
                "/api/v1/**", "/actuator/health", "/actuator/info").permitAll()
            .anyRequest().authenticated()
        )
        .httpBasic(AbstractHttpConfigurer::disable)
        .build();
}
```

---

## Environment Variables

| Variable | Default | Description |
|---|---|---|
| `AICHAT_DB_HOST` | `localhost` | PostgreSQL host |
| `AICHAT_DB_PORT` | `5437` | PostgreSQL port (local dev); `5432` inside Docker Compose network |
| `AICHAT_DB_NAME` | `ai_chat` | Database name |
| `AICHAT_DB_USERNAME` | `ai_chat` | DB user |
| `AICHAT_DB_PASSWORD` | `ai_chat` | DB password |
| `AICHAT_CHAT_BASE_URL` | `http://localhost:11434` | Ollama base URL (no `/v1` suffix — Spring AI Ollama provider) |
| `AICHAT_CHAT_API_KEY` | `ollama` | API key placeholder for Ollama |
| `AICHAT_CHAT_MODEL` | `gemma4:31b-cloud` | Primary chat model |
| `AICHAT_CHAT_ALT_MODEL` | `gemma4:31b-cloud` | Alternative chat model |
| `AICHAT_TOOL_MODEL` | `gemma4:31b-cloud` | Tool-calling model |
| `MCP_MEDICAL_URL` | `http://localhost:8092/sse` | ai-architect-6-mcp SSE endpoint (bootstrap seeder) |
| `SERVER_PORT` | `8095` | Application port |
| `SPRING_PROFILES_ACTIVE` | _(none)_ | Set to `prod` for production actuator defaults |
| `MANAGEMENT_ENDPOINT_HEALTH_SHOW_DETAILS` | `never` | Override to `always` in dev for component details |

---

## Docker Compose

See repository root `docker-compose.yml` (PostgreSQL + ai-chat). Ollama and MCP run on the host via `host.docker.internal`.

```yaml
services:
  ai-chat:
    build: .
    ports:
      - "8095:8095"
    environment:
      AICHAT_DB_HOST: postgres
      AICHAT_DB_PORT: 5432
      AICHAT_DB_USERNAME: ai_chat
      AICHAT_DB_PASSWORD: ai_chat
      AICHAT_CHAT_BASE_URL: http://host.docker.internal:11434
      AICHAT_CHAT_API_KEY: ollama
      AICHAT_CHAT_MODEL: ${AICHAT_CHAT_MODEL:-gemma4:31b-cloud}
      MCP_MEDICAL_URL: ${MCP_MEDICAL_URL:-http://host.docker.internal:8092/sse}
    depends_on:
      postgres:
        condition: service_healthy
    extra_hosts:
      - "host.docker.internal:host-gateway"
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8095/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s

  postgres:
    image: postgres:17
    environment:
      POSTGRES_DB: ai_chat
      POSTGRES_USER: ai_chat
      POSTGRES_PASSWORD: ai_chat
    volumes:
      - pgdata:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ai_chat -d ai_chat"]
      interval: 10s
      timeout: 5s
      retries: 5

volumes:
  pgdata:
```

> Ollama runs on the host, not in Docker. `host.docker.internal` resolves correctly on Linux with `extra_hosts: host.docker.internal:host-gateway` or Docker Desktop on Mac/Windows.

---

## Dockerfile

```dockerfile
# Multi-stage build
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests -B

FROM eclipse-temurin:21-jre-jammy
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=builder /build/target/*.jar app.jar
EXPOSE 8095
HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
  CMD curl -f http://localhost:8095/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

## MCP Client Connection (optional)

MCP enrichment is **optional**. The chat application starts and operates normally without ai-architect-6-mcp. Connect the MCP server only when tool enrichment is needed (phase 2).

### ai-architect-6-mcp (phase 2)

To enable MCP enrichment, [ai-architect-6-mcp](https://github.com/berdachuk/ai-architect-6-mcp) (`medical-mcp-server`) should be running on port `8092`. If it is not running, ai-chat still starts; the health indicator reports MCP DOWN and chat proceeds without tools.

```bash
cd ai-architect-6-mcp
docker compose up -d
# or: mvn spring-boot:run
```

Verify connection:

```bash
curl http://localhost:8092/actuator/health
# Expected: {"status":"UP"}
```

### Adding additional MCP servers

Add new entries under `spring.ai.mcp.client.sse.connections`:

```yaml
spring:
  ai:
    mcp:
      client:
        sse:
          connections:
            medical-dataset:
              url: http://localhost:8092/sse
              tools: true
              resources: true
              prompts: true
            weather-service:
              url: http://localhost:8093/sse
              tools: true
            document-search:
              url: http://localhost:8094/sse
              tools: true
              resources: true
```

Each connection is initialized on startup. Tools from all connected servers are available to the LLM.

---

## Ollama Setup

### Pull required models

```bash
ollama pull gemma4:31b-cloud
ollama pull gemma4:12b
ollama pull functiongemma:270m
```

### Verify Ollama is running

```bash
curl http://localhost:11434/api/tags
# Expected: list of available models including gemma4:31b-cloud
```

### Test OpenAI-compatible endpoint

```bash
curl http://localhost:11434/v1/models
# Expected: JSON list of models
```

---

## Local Development Setup

### Prerequisites

- JDK 21
- Maven 3.9+
- Docker (PostgreSQL local dev + **Testcontainers** for IT, DEC-009)
- Ollama with `gemma4:31b-cloud`, `gemma4:12b`, and `functiongemma:270m` pulled (M3+)

### Windows development (DEC-008)

Use **WSL 2** for Docker. Clone the repo in WSL and run `mvn`, `docker`, and `mvn verify -Pintegration` from the WSL shell.

### Start PostgreSQL

```bash
docker run -d --name ai-chat-postgres \
  -e POSTGRES_DB=ai_chat \
  -e POSTGRES_USER=ai_chat \
  -e POSTGRES_PASSWORD=ai_chat \
  -p 5437:5432 \
  postgres:17
```

### Start the application

```bash
# From repository root (ai-architect-6-chat)
mvn spring-boot:run
```

### Start with MCP server (phase 2)

```bash
# Terminal 1 — ai-architect-6-mcp
cd ../ai-architect-6-mcp && mvn spring-boot:run

# Terminal 2 — ai-architect-6-chat
mvn spring-boot:run
```

### Access the chat

Open `http://localhost:8095/` in a browser.

---

## Production Considerations

| Area | Recommendation |
|---|---|
| Database | Use managed PostgreSQL with automated backups |
| LLM | Use dedicated Ollama instance or cloud LLM provider |
| MCP servers | Deploy alongside or on accessible network |
| Security | Replace permit-all with JWT/OAuth2 (`oauth2ResourceServer`) |
| User identity | Replace `X-User-Id` header with authenticated principal |
| Rate limiting | Add token bucket filter on `/api/v1/**` |
| Monitoring | Enable Prometheus metrics export, add Grafana dashboards |
| Logging | Structured JSON logging for log aggregation |
| Session memory | Configure compaction thresholds for production load |

---

## Related documentation

- [README.md](README.md) — documentation index and naming
- [../README.md](../README.md) — project overview
- [01-requirements.md](01-requirements.md) — env vars (§13) and configuration (§12)
- [02-architecture.md](02-architecture.md) — stack and security defaults
- [03-design.md](03-design.md) — service and MCP implementation
- [04-testing.md](04-testing.md) — smoke checklist (M9)
