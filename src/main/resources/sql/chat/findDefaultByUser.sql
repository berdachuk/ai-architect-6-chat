SELECT id, user_id, name, agent_id, is_default, created_at, updated_at, last_activity_at, message_count
FROM ai_chat.chat
WHERE user_id = :userId AND is_default = TRUE
