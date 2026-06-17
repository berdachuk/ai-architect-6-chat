UPDATE ai_chat.harness_workflow_run
SET state = :state, updated_at = now()
WHERE run_id = :runId
