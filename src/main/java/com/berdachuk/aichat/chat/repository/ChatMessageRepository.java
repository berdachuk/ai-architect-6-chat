package com.berdachuk.aichat.chat.repository;

import com.berdachuk.aichat.chat.domain.ChatMessage;

import java.util.List;

public interface ChatMessageRepository {

    List<ChatMessage> findByChatId(String chatId, int limit, int offset);

    ChatMessage insert(ChatMessage message);

    int getNextSequenceNumber(String chatId);

    void softDeleteByChatId(String chatId);

    int countByChatId(String chatId);
}
