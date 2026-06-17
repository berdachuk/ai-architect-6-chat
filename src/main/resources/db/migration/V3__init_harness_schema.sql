CREATE TABLE IF NOT EXISTS ai_chat.harness_workflow_run (
    run_id      CHAR(24)     PRIMARY KEY,
    session_id  VARCHAR(512) NOT NULL,
    state       VARCHAR(50)  NOT NULL,
    plan_json   JSONB,
    created_at  TIMESTAMPTZ  DEFAULT now(),
    updated_at  TIMESTAMPTZ  DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_harness_workflow_run_session
    ON ai_chat.harness_workflow_run (session_id, created_at DESC);

CREATE TABLE IF NOT EXISTS ai_chat.harness_chain_trace (
    id          CHAR(24)     PRIMARY KEY,
    run_id      CHAR(24)     NOT NULL REFERENCES ai_chat.harness_workflow_run(run_id) ON DELETE CASCADE,
    event_type  VARCHAR(50)  NOT NULL,
    payload     JSONB,
    created_at  TIMESTAMPTZ  DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_harness_chain_trace_run
    ON ai_chat.harness_chain_trace (run_id, created_at);
