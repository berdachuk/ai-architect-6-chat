UPDATE ai_chat.chat
SET last_activity_at = :lastActivityAt, message_count = :messageCount, updated_at = now()
WHERE id = :id
