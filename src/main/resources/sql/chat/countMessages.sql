SELECT COUNT(*) FROM ai_chat.chat_message WHERE chat_id = :chatId AND deleted_at IS NULL
