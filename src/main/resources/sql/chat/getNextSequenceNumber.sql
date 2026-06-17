SELECT COALESCE(MAX(sequence_number), 0) + 1 FROM ai_chat.chat_message WHERE chat_id = :chatId
