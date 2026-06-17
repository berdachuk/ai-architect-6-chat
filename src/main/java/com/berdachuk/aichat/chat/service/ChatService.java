package com.berdachuk.aichat.chat.service;

import com.berdachuk.aichat.chat.domain.Chat;
import com.berdachuk.aichat.chat.domain.ChatMessage;

import java.util.List;
import java.util.Map;

public interface ChatService {

    Chat getOrCreateDefaultChat(String userId);

    Chat createChat(String userId, String name, String agentId);

    List<Chat> listChats(String userId);

    Chat requireOwnedChat(String userId, String chatId);

    void deleteChat(String userId, String chatId);

    void renameChat(String userId, String chatId, String name);

    List<ChatMessage> getHistory(String userId, String chatId, int limit, int offset);

    ChatMessage appendUserMessage(String chatId, String content);

    ChatMessage appendAssistantMessage(String chatId, String content, int tokensUsed, Map<String, Object> metadata);

    void updateAgentId(String chatId, String agentId);
}
