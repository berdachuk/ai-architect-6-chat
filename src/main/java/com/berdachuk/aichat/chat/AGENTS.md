# AGENTS.md — `chat` module

**Package:** `com.berdachuk.aichat.chat`  
**Depends on:** `core` only  
**Canonical design:** [docs/03-design.md](../../../../../../../docs/03-design.md) (schema, repositories, ChatService)

## Purpose

Chat session and message persistence plus REST API (`/api/v1/chats`). Owns the chat domain — not LLM orchestration (see `llm/`).

## Domain models (owned here)

| Model         | Key fields                                                                                          |
|---------------|-----------------------------------------------------------------------------------------------------|
| `Chat`        | `id`, `userId`, `name`, `agentId`, `isDefault`, `messageCount`, `enabledMcpConnections`, timestamps |
| `ChatMessage` | `id`, `chatId`, `role`, `content`, `sequenceNumber`, `metadata`                                     |

Session memory for LLM uses Spring AI JDBC (`userId-chatId`) — owned by `llm/`, not duplicated here.

## Layout

```text
chat/
├── domain/          Chat.java, ChatMessage.java (records)
├── repository/      interfaces + impl/ (JDBC, @InjectSql)
├── service/         ChatService + impl/
└── rest/            ChatController
```

## Conventions

- 24-char hex IDs via `core.util.IdGenerator`
- SQL in `src/main/resources/sql/chat/*.sql` — `@InjectSql` injection; **named binds only** (`:id`, etc.) per DEC-013
- `requireOwnedChat(userId, chatId)` for all mutations
- Soft-delete messages on chat delete; recreate default chat if none left
- REST returns domain records or DTOs — no JDBC types at boundary

## Commands

Same as root (`mvn test`); module tests under `src/test/java/.../chat/`.

## Boundaries

|    |                                                                 |
|----|-----------------------------------------------------------------|
| ✅  | CRUD, history pagination, rename, default chat lifecycle        |
| 🚫 | LLM calls, MCP, Harness, Thymeleaf (delegate to `llm/`, `web/`) |
| 🚫 | Direct dependency on `llm`, `mcp`, `web`                        |

## Skills

- [code-style](../../../../../../../.agents/skills/code-style/SKILL.md)
- [testing](../../../../../../../.agents/skills/testing/SKILL.md)
