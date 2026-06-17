UPDATE ai_chat.chat
SET enabled_mcp_connections = CAST(:enabledMcpConnections AS JSONB),
    updated_at = now()
WHERE id = :id
