INSERT INTO ai_chat.chat (
    id, user_id, name, agent_id, is_default, created_at, updated_at, last_activity_at, message_count,
    enabled_mcp_connections)
VALUES (
    :id, :userId, :name, :agentId, :isDefault, :createdAt, :updatedAt, :lastActivityAt, :messageCount,
    CAST(:enabledMcpConnections AS JSONB))
