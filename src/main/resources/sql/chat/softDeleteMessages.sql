UPDATE ai_chat.chat_message SET deleted_at = now() WHERE chat_id = :chatId AND deleted_at IS NULL
