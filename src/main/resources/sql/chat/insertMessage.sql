INSERT INTO ai_chat.chat_message (id, chat_id, role, content, sequence_number, tokens_used, created_at, metadata)
VALUES (:id, :chatId, :role, :content, :sequenceNumber, :tokensUsed, :createdAt, CAST(:metadata AS jsonb))
