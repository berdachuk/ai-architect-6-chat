INSERT INTO ai_chat.harness_chain_trace (id, run_id, event_type, payload, created_at)
VALUES (:id, :runId, :eventType, CAST(:payload AS JSONB), now())
