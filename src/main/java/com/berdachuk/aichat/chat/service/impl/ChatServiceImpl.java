package com.berdachuk.aichat.chat.service.impl;

import com.berdachuk.aichat.chat.domain.Chat;
import com.berdachuk.aichat.chat.domain.ChatMessage;
import com.berdachuk.aichat.chat.exception.ChatNotFoundException;
import com.berdachuk.aichat.chat.repository.ChatMessageRepository;
import com.berdachuk.aichat.chat.repository.ChatRepository;
import com.berdachuk.aichat.chat.service.ChatService;
import com.berdachuk.aichat.core.util.IdGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class ChatServiceImpl implements ChatService {

    private final ChatRepository chatRepository;
    private final ChatMessageRepository messageRepository;

    public ChatServiceImpl(ChatRepository chatRepository, ChatMessageRepository messageRepository) {
        this.chatRepository = chatRepository;
        this.messageRepository = messageRepository;
    }

    @Override
    public Chat getOrCreateDefaultChat(String userId) {
        return chatRepository.findDefaultByUserId(userId)
                .orElseGet(() -> createChat(userId, "New Chat", "auto"));
    }

    @Override
    public Chat createChat(String userId, String name, String agentId) {
        Instant now = Instant.now();
        Chat chat = new Chat(
                IdGenerator.generateId(),
                userId,
                name != null ? name : "New Chat",
                agentId != null ? agentId : "auto",
                !chatRepository.existsByUserId(userId),
                now,
                now,
                now,
                0);
        return chatRepository.insert(chat);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Chat> listChats(String userId) {
        return chatRepository.findByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Chat requireOwnedChat(String userId, String chatId) {
        return chatRepository.findById(chatId)
                .filter(chat -> chat.userId().equals(userId))
                .orElseThrow(() -> new ChatNotFoundException(chatId));
    }

    @Override
    public void deleteChat(String userId, String chatId) {
        requireOwnedChat(userId, chatId);
        messageRepository.softDeleteByChatId(chatId);
        chatRepository.deleteById(chatId);
        if (chatRepository.findByUserId(userId).isEmpty()) {
            createChat(userId, "New Chat", "auto");
        }
    }

    @Override
    public void renameChat(String userId, String chatId, String name) {
        requireOwnedChat(userId, chatId);
        chatRepository.updateName(chatId, name);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatMessage> getHistory(String userId, String chatId, int limit, int offset) {
        requireOwnedChat(userId, chatId);
        return messageRepository.findByChatId(chatId, limit, offset);
    }

    @Override
    public ChatMessage appendUserMessage(String chatId, String content) {
        int seq = messageRepository.getNextSequenceNumber(chatId);
        ChatMessage message = new ChatMessage(
                IdGenerator.generateId(),
                chatId,
                ChatMessage.ROLE_USER,
                content,
                seq,
                null,
                Instant.now(),
                Map.of());
        ChatMessage saved = messageRepository.insert(message);
        chatRepository.updateActivity(chatId, Instant.now(), seq);
        return saved;
    }

    @Override
    public ChatMessage appendAssistantMessage(
            String chatId, String content, int tokensUsed, Map<String, Object> metadata) {
        int seq = messageRepository.getNextSequenceNumber(chatId);
        ChatMessage message = new ChatMessage(
                IdGenerator.generateId(),
                chatId,
                ChatMessage.ROLE_ASSISTANT,
                content,
                seq,
                tokensUsed,
                Instant.now(),
                metadata != null ? metadata : Map.of());
        ChatMessage saved = messageRepository.insert(message);
        chatRepository.updateActivity(chatId, Instant.now(), seq);
        return saved;
    }

    @Override
    public void updateAgentId(String chatId, String agentId) {
        chatRepository.findById(chatId)
                .orElseThrow(() -> new ChatNotFoundException(chatId));
        chatRepository.updateAgentId(chatId, agentId);
    }
}
