# Deployment
## Operations & deployment guide

**Version:** 1.0.0
**Requirements:** [01-requirements.md](01-requirements.md) · [02-architecture.md](02-architecture.md)

---

## Application Configuration

### `application.yml`

```yaml
spring:
  application:
    name: ai-chat
  autoconfigure:
    exclude:
      - org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration
      - org.springframework.ai.model.openai.autoconfigure.OpenAiEmbeddingAutoConfiguration
      - org.springframework.ai.model.openai.autoconfigure.OpenAiAudioSpeechAutoConfiguration
      - org.springframework.ai.model.openai.autoconfigure.OpenAiAudioTranscriptionAutoConfiguration
      - org.springframework.ai.model.openai.autoconfigure.OpenAiImageAutoConfiguration
  datasource:
    url: jdbc:postgresql://${AICHAT_DB_HOST:localhost}:5432/${AICHAT_DB_NAME:ai_chat}
    username: ${AICHAT_DB_USERNAME:ai_chat}
    password: ${AICHAT_DB_PASSWORD:ai_chat}
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: 10
      minimum-idle: 3
      connection-timeout: 20000
      idle-timeout: 300000
      max-lifetime: 1800000
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
    default-schema: ai_chat
  ai:
    openai:
      enabled: false
    custom:
      chat:
        provider: openai
        base-url: ${CHAT_BASE_URL:http://localhost:11434/v1}
        api-key: ${CHAT_API_KEY:none}
        model: ${CHAT_MODEL:gemma4:31b-cloud}
        temperature: 0.7
        max-tokens: 6000
      chat-alt:
        provider: openai
        base-url: ${CHAT_ALT_BASE_URL:http://localhost:11434/v1}
        api-key: ${CHAT_ALT_API_KEY:none}
        model: ${CHAT_ALT_MODEL:gemma4:12b}
        temperature: 0.7
        max-tokens: 6000
      tool-calling:
        provider: openai
        base-url: ${TOOL_CALLING_BASE_URL:http://localhost:11434/v1}
        api-key: ${TOOL_CALLING_API_KEY:none}
        model: ${TOOL_CALLING_MODEL:functiongemma:270m}
        temperature: 0.1
        max-tokens: 2048
    mcp:
      client:
        sse:
          connections:
            medical-dataset:
              url: ${MCP_MEDICAL_URL:http://localhost:8092/sse}
              tools: true
              resources: true
              prompts: true
    session:
      jdbc:
        schema: ai_chat
        compaction:
          strategy: turn-window
          max-turns: 20
          max-tokens: 4000
          window-turns: 30

server:
  port: ${SERVER_PORT:8080}
  shutdown: graceful
  compression:
    enabled: true
    mime-types: application/json, text/event-stream
    min-response-size: 1024

management:
  endpoints:
    web:
      exposure:
        include: health, info, metrics
  endpoint:
    health:
      show-details: always
      probes:
        enabled: true

ai-chat:
  harness:
    max-iterations: 2
    retry-on-verify-fail: true
    policy-gate-enabled: true
    human-checkpoint-enabled: false
  features:
    mcp-client: true
    harness: true
    structured-output: true

logging:
  level:
    com.berdachuk.aichat: INFO
    com.berdachuk.aichat.llm: DEBUG
    com.berdachuk.aichat.mcp: DEBUG
    org.springframework.ai.mcp: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
```

### Security

For local / dev use (matching `med-expert-match-ce` local profile):

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
| `AICHAT_DB_NAME` | `ai_chat` | Database name |
| `AICHAT_DB_USERNAME` | `ai_chat` | DB user |
| `AICHAT_DB_PASSWORD` | `ai_chat` | DB password |
| `CHAT_BASE_URL` | `http://localhost:11434/v1` | Primary chat model endpoint (must include `/v1` for Ollama) |
| `CHAT_API_KEY` | `none` | API key for chat model (Ollama doesn't require one) |
| `CHAT_MODEL` | `gemma4:31b-cloud` | Primary chat model name |
| `CHAT_ALT_BASE_URL` | `http://localhost:11434/v1` | Alternative chat model endpoint |
| `CHAT_ALT_API_KEY` | `none` | API key for alt model |
| `CHAT_ALT_MODEL` | `gemma4:12b` | Alternative chat model name |
| `TOOL_CALLING_BASE_URL` | `http://localhost:11434/v1` | Tool-calling model endpoint |
| `TOOL_CALLING_API_KEY` | `none` | API key for tool-calling model |
| `TOOL_CALLING_MODEL` | `functiongemma:270m` | Tool-calling model name |
| `MCP_MEDICAL_URL` | `http://localhost:8092/sse` | ai-architect-6-mcp SSE endpoint (`medical-dataset` connection) |
| `SERVER_PORT` | `8080` | Application port |

---

## Docker Compose

```yaml
services:
  ai-chat:
    build: .
    ports:
      - "8080:8080"
    environment:
      AICHAT_DB_HOST: postgres
      AICHAT_DB_USERNAME: ai_chat
      AICHAT_DB_PASSWORD: ai_chat
      CHAT_BASE_URL: http://host.docker.internal:11434/v1
      CHAT_API_KEY: none
      CHAT_MODEL: gemma4:31b-cloud
      CHAT_ALT_BASE_URL: http://host.docker.internal:11434/v1
      CHAT_ALT_API_KEY: none
      CHAT_ALT_MODEL: gemma4:12b
      TOOL_CALLING_BASE_URL: http://host.docker.internal:11434/v1
      TOOL_CALLING_API_KEY: none
      TOOL_CALLING_MODEL: functiongemma:270m
      MCP_MEDICAL_URL: http://host.docker.internal:8092/sse
    depends_on:
      postgres:
        condition: service_healthy
    extra_hosts:
      - host.docker.internal:host-gateway
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
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
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1
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
- Docker (for PostgreSQL)
- Ollama with `gemma4:31b-cloud`, `gemma4:12b`, and `functiongemma:270m` pulled

### Start PostgreSQL

```bash
docker run -d --name ai-chat-postgres \
  -e POSTGRES_DB=ai_chat \
  -e POSTGRES_USER=ai_chat \
  -e POSTGRES_PASSWORD=ai_chat \
  -p 5432:5432 \
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

Open `http://localhost:8080/` in a browser.

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
