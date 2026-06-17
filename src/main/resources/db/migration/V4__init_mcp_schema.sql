ALTER TABLE ai_chat.chat
    ADD COLUMN IF NOT EXISTS enabled_mcp_connections JSONB NOT NULL DEFAULT '[]'::jsonb;

CREATE TABLE ai_chat.mcp_connection (
    id               CHAR(24)     PRIMARY KEY,
    name             VARCHAR(255) NOT NULL,
    url              VARCHAR(2048) NOT NULL,
    tools_enabled    BOOLEAN      NOT NULL DEFAULT TRUE,
    resources_enabled BOOLEAN     NOT NULL DEFAULT FALSE,
    prompts_enabled  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX idx_mcp_connection_name ON ai_chat.mcp_connection (name);
