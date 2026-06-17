#!/usr/bin/env bash
# REST smoke checks against a running ai-chat instance (docs/04-testing.md §7).
set -euo pipefail

BASE_URL="${1:-http://localhost:8095}"
USER_ID="${SMOKE_USER_ID:-smoke-cli}"

echo "Smoke: health"
curl -sf "${BASE_URL}/actuator/health" | grep -q '"status":"UP"'

echo "Smoke: home page"
curl -sf "${BASE_URL}/" | grep -q 'AI Chat'

echo "Smoke: chat API"
CHAT_ID=$(curl -sf -X POST "${BASE_URL}/api/v1/chats" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: ${USER_ID}" \
  -d '{"name":"Smoke","agentId":"auto"}' | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
test -n "${CHAT_ID}"

curl -sf -H "X-User-Id: ${USER_ID}" "${BASE_URL}/api/v1/chats" | grep -q 'Smoke'

echo "Smoke: stream (stub LLM in test profile only — requires running app with Ollama or stub)"
curl -sf -X POST "${BASE_URL}/api/v1/chats/${CHAT_ID}/messages/stream" \
  -H "Content-Type: application/json" \
  -H "Accept: text/event-stream" \
  -H "X-User-Id: ${USER_ID}" \
  -d '{"content":"hello"}' | grep -q 'event:done'

echo "All REST smoke checks passed."
