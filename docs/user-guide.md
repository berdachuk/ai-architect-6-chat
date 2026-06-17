# AI Chat — User Guide

**Version:** 1.0.0  
**Application:** `ai-chat` (ai-architect-6-chat)

This guide explains how to use the AI Chat web application: starting a conversation, managing sessions, optional MCP tool enrichment, and reading agent progress.

For installation and operations, see [05-deployment.md](05-deployment.md). For developers, see [README.md](../README.md).

---

## 1. What is AI Chat?

AI Chat is a browser-based assistant that:

- Streams answers token-by-token as they are generated
- Keeps **multiple chat sessions** (like separate conversation threads)
- Remembers context within a session across many turns
- Optionally connects to **MCP servers** for tool-assisted answers (e.g. querying a medical dataset)
- Shows **agent progress** while the assistant plans, calls tools, and responds

**Important:** Chat works without any MCP server. If MCP is unavailable, you still get normal LLM responses.

---

## 2. Before you start

### What you need

| Requirement | Notes |
|---|---|
| Running **ai-chat** instance | Default URL: `http://localhost:8095` |
| **Ollama** (or configured LLM backend) | Required for real assistant replies |
| **PostgreSQL** | Handled automatically with Docker Compose |
| **MCP server** (optional) | e.g. [ai-architect-6-mcp](https://github.com/berdachuk/ai-architect-6-mcp) on port `8092` |

### Quick start (Docker)

```bash
docker compose up --build
```

Open **http://localhost:8095/** in your browser.

Verify the app is healthy:

```bash
curl http://localhost:8095/actuator/health
```

Expected: `"status":"UP"`.

---

## 3. Opening the app

1. Open your browser and go to `http://localhost:8095/` (or your deployment URL).
2. You land on your **default chat** — usually named **New Chat**.
3. The page loads your **chat history** for that session automatically.

On first visit, the sidebar may be empty until the app creates a default session.

---

## 4. Screen layout

```text
┌─────────────────────────────────────────────────────────────┐
│  AI Chat                                    [+ New Chat]    │
├──────────────┬──────────────────────────────────────────────┤
│  Sidebar     │  Message area                                │
│              │  (your conversation)                         │
│  • Chat list │                                              │
│  • Rename ✎  │                                              │
│  • Delete ×  │                                              │
│              ├──────────────────────────────────────────────┤
│  MCP Tools   │  Agent Progress (collapsible, during reply)  │
│  (optional)  ├──────────────────────────────────────────────┤
│              │  [ Type your message...        ] [ Send ]    │
│ [Delete All] │                                              │
└──────────────┴──────────────────────────────────────────────┘
```

| Area | Purpose |
|---|---|
| **Sidebar — chat list** | Switch between sessions; see message counts |
| **MCP Tools panel** | Enable/disable tool servers **for the current chat** |
| **Message area** | Conversation history (Markdown rendered for assistant replies) |
| **Agent Progress** | Live steps: planning, tool calls, pipeline stages |
| **Composer** | Type and send messages |

---

## 5. Sending messages

1. Click the text box at the bottom (**Type your message...**).
2. Type your question or instruction.
3. Click **Send** or press **Enter**.

| Key | Action |
|---|---|
| **Enter** | Send message |
| **Shift + Enter** | New line in the composer (do not send yet) |

### What happens when you send

1. Your message appears immediately on the right (**You**).
2. The assistant placeholder appears on the left (**AI**).
3. The **Agent Progress** panel opens.
4. Tokens stream into the assistant bubble as they arrive.
5. When finished, the composer is re-enabled and the progress panel collapses to a summary.

Assistant replies support **Markdown** (headings, lists, code blocks, links). Content is sanitized for safe display.

### If streaming fails

You may see `[Error: could not complete stream]` in the reply. Common causes:

- Ollama is not running or the configured model is not pulled
- Network timeout between ai-chat and the LLM backend
- Server overload

Check [Section 10 — Troubleshooting](#10-troubleshooting).

---

## 6. Managing chat sessions

### Create a new chat

- Click **+ New Chat** in the top navigation bar.
- A new session opens; the sidebar lists all your chats.

### Switch chats

- Click any chat name in the sidebar.
- History for that session loads in the message area.

### Rename a chat

1. In the sidebar, click the **✎** (pencil) next to the chat name.
2. Enter a new name in the prompt.
3. The browser tab title updates if you renamed the active chat.

### Delete one chat

1. Click **×** next to the chat in the sidebar.
2. Confirm **Delete this chat?**
3. If you deleted the active chat, you are returned to the home page (a new default chat is created if needed).

### Delete all chats

1. Click **Delete All Chats** at the bottom of the sidebar.
2. Confirm the dialog.
3. You are redirected home; the app recreates a default **New Chat**.

### Default chat behavior

- Every user has at least one chat.
- If you delete your last chat, the app automatically creates a new default session named **New Chat**.

---

## 7. Session memory

AI Chat remembers earlier turns **within the same session** so follow-up questions work naturally (e.g. “What did I ask first?”).

Memory is managed automatically with turn-window compaction (roughly the last 20 turns / 4000 tokens, configurable by operators). You do not need to configure anything.

**Tip:** Start a **new chat** when changing topic completely — this gives the assistant a clean context window.

---

## 8. MCP tools (optional)

MCP (Model Context Protocol) servers expose **tools** the assistant can call — for example, searching a dataset or listing specialties.

### MCP is optional

- Chat works with **no MCP servers** running.
- If a server is down, chat continues; only tool enrichment is unavailable.

### MCP Tools panel

At the bottom of the sidebar:

| Element | Meaning |
|---|---|
| **Connection name** | Registered MCP server (e.g. `medical-dataset`) |
| **Checkbox** | Enable tools from this server **for the current chat only** |
| **Status** | `UP` = reachable, `DOWN` = server unavailable |
| **Badge** (e.g. `1/1 up`) | How many servers in the catalog are reachable |

Toggle checkboxes to control which servers the assistant may use in **this** chat. Other chats keep their own selections.

### Example with medical MCP

If [ai-architect-6-mcp](https://github.com/berdachuk/ai-architect-6-mcp) is running on `:8092`:

1. Ensure the connection shows **UP** in the MCP panel.
2. Check the box for `medical-dataset`.
3. Ask questions that benefit from tools, for example:
   - “List medical specialties”
   - “Search for cardiovascular cases”

When tools run, the **Agent Progress** panel shows entries like `🔧 Tool [medical-dataset]: list_specialties (done)`.

### Adding MCP servers (operators)

Administrators register servers via the REST API (`POST /api/v1/mcp/connections`) or bootstrap configuration. End users only toggle servers that already appear in the catalog.

---

## 9. Agent progress panel

While the assistant works on your request, the **Agent Progress** panel shows what is happening behind the scenes.

| Icon / label | Meaning |
|---|---|
| **Pipeline stages** (▶ ✓ ✗) | Harness workflow steps (planning, execution, verification) |
| **🔧 Tool** | MCP tool invocation (running / done / failed) |
| **💭** | Reasoning step |
| **📋 Plan** | Planned sub-steps |
| **🤖 LLM call** | Model invocation |

After the reply completes, the panel collapses to a short summary (e.g. `▸ 3 step(s) completed — click to expand`).

Click **+** / **−** to expand or collapse the detail list.

---

## 10. Troubleshooting

| Problem | What to try |
|---|---|
| Page does not load | Confirm ai-chat is running; check `http://localhost:8095/actuator/health` |
| No assistant reply / stream error | Start Ollama; pull the configured model (default `gemma3:4b`) |
| Slow first reply | Model may be loading into memory — wait and retry |
| MCP shows `DOWN` | Start the MCP server; check URL (default `http://localhost:8092/sse`) |
| MCP checkbox has no effect | Server must be `UP`; only enabled servers are used for that chat |
| Chats from another user appear missing | Sessions are per user identity (see below) |
| Lost all chats after “Delete All” | Expected — a fresh default **New Chat** is created |

### User identity (current release)

**Default:** no login required. Your user id is **`anonymous`** unless you set:

- HTTP header `X-User-Id: your-name`, or
- cookie `aichat-user-id`

The web UI sends `X-User-Id` automatically. Different values isolate chat histories.

**Optional OAuth2 (operators):** enable profile `oauth2` to require JWT on `/api/v1/**` while keeping the web UI usable for staged testing. See [05-deployment.md](05-deployment.md).

---

## 11. Tips for better results

1. **Be specific** — clear questions get clearer answers.
2. **Use new chats for new topics** — avoids confusion from old context.
3. **Enable MCP only when needed** — simpler questions do not require tools.
4. **Watch Agent Progress** — if a tool fails, rephrase or check MCP server health.
5. **Wait for streaming to finish** — the Send button is disabled until the current reply completes.

---

## 12. REST API (power users)

The web UI uses the same API you can call from scripts:

| Action | Method | Path |
|---|---|---|
| List chats | `GET` | `/api/v1/chats` |
| Create chat | `POST` | `/api/v1/chats` |
| Message history | `GET` | `/api/v1/chats/{id}/history` |
| Rename | `PUT` | `/api/v1/chats/{id}/name` |
| Delete | `DELETE` | `/api/v1/chats/{id}` |
| Send (stream) | `POST` | `/api/v1/chats/{id}/messages/stream` |
| MCP selection | `GET` / `PUT` | `/api/v1/chats/{id}/mcp` |
| MCP catalog | `GET` | `/api/v1/mcp/connections` |

Include header `X-User-Id: your-id` (or set the `aichat-user-id` cookie).

Quick smoke test against a running instance:

```bash
bash scripts/smoke-rest.sh http://localhost:8095
```

---

## Related documentation

| Document | Audience |
|---|---|
| [05-deployment.md](05-deployment.md) | Operators — Docker, env vars, Ollama |
| [04-testing.md](04-testing.md) | QA — smoke checklist |
| [01-requirements.md](01-requirements.md) | Product / engineering — full requirements |
