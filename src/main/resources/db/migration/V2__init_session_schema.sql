-- Spring AI Session JDBC (community) — ai_chat schema
CREATE TABLE IF NOT EXISTS ai_chat.ai_session (
    id            VARCHAR(255)  NOT NULL PRIMARY KEY,
    user_id       VARCHAR(255)  NOT NULL,
    created_at    TIMESTAMP     NOT NULL,
    expires_at    TIMESTAMP,
    metadata      TEXT,
    event_version BIGINT        NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_ai_session_user_id
    ON ai_chat.ai_session (user_id);

CREATE INDEX IF NOT EXISTS idx_ai_session_expires_at
    ON ai_chat.ai_session (expires_at);

CREATE TABLE IF NOT EXISTS ai_chat.ai_session_event (
    id              VARCHAR(255)  NOT NULL PRIMARY KEY,
    session_id      VARCHAR(255)  NOT NULL,
    "timestamp"     TIMESTAMP     NOT NULL,
    message_type    VARCHAR(20)   NOT NULL,
    message_content TEXT,
    message_data    TEXT,
    synthetic       BOOLEAN       NOT NULL DEFAULT FALSE,
    branch          VARCHAR(500),
    metadata        TEXT,
    CONSTRAINT fk_ai_session_event_session
        FOREIGN KEY (session_id) REFERENCES ai_chat.ai_session (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_ai_session_event_session_ts
    ON ai_chat.ai_session_event (session_id, "timestamp");
