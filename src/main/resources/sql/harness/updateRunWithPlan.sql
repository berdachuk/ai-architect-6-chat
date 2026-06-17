UPDATE ai_chat.harness_workflow_run
SET state = :state, plan_json = CAST(:planJson AS JSONB), updated_at = now()
WHERE run_id = :runId
