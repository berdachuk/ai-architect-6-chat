CREATE SCHEMA IF NOT EXISTS ai_chat;

CREATE TABLE ai_chat.chat (
    id               CHAR(24)     PRIMARY KEY,
    user_id          VARCHAR(255) NOT NULL,
    name             VARCHAR(255),
    agent_id         VARCHAR(50)  DEFAULT 'auto',
    is_default       BOOLEAN      DEFAULT FALSE,
    created_at       TIMESTAMPTZ  DEFAULT now(),
    updated_at       TIMESTAMPTZ  DEFAULT now(),
    last_activity_at TIMESTAMPTZ  DEFAULT now(),
    message_count    INT          DEFAULT 0
);

CREATE UNIQUE INDEX idx_chat_user_default
    ON ai_chat.chat (user_id, is_default) WHERE is_default = TRUE;

CREATE INDEX idx_chat_user_activity
    ON ai_chat.chat (user_id, last_activity_at DESC);

CREATE TABLE ai_chat.chat_message (
    id              CHAR(24)     PRIMARY KEY,
    chat_id         CHAR(24)     NOT NULL REFERENCES ai_chat.chat(id) ON DELETE CASCADE,
    role            VARCHAR(20)  NOT NULL CHECK (role IN ('user', 'assistant', 'system')),
    content         TEXT         NOT NULL,
    sequence_number INT          NOT NULL,
    tokens_used     INT,
    created_at      TIMESTAMPTZ  DEFAULT now(),
    metadata        JSONB,
    deleted_at      TIMESTAMPTZ
);

CREATE UNIQUE INDEX idx_chat_message_seq
    ON ai_chat.chat_message (chat_id, sequence_number);

CREATE INDEX idx_chat_message_chat
    ON ai_chat.chat_message (chat_id, created_at);
