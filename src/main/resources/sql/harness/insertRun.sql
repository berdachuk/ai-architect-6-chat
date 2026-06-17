INSERT INTO ai_chat.harness_workflow_run (run_id, session_id, state, plan_json, created_at, updated_at)
VALUES (:runId, :sessionId, :state, CAST(:planJson AS JSONB), now(), now())
