SELECT id, user_id, name, agent_id, is_default, created_at, updated_at, last_activity_at, message_count,
       enabled_mcp_connections
FROM ai_chat.chat
WHERE id = :id
