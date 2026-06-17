package com.berdachuk.aichat.chat.repository;

import com.berdachuk.aichat.chat.domain.Chat;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ChatRepository {

    Optional<Chat> findById(String id);

    List<Chat> findByUserId(String userId);

    Optional<Chat> findDefaultByUserId(String userId);

    Chat insert(Chat chat);

    void updateName(String id, String name);

    void updateAgentId(String id, String agentId);

    void updateActivity(String id, Instant lastActivityAt, int messageCount);

    void updateEnabledMcpConnections(String id, List<String> connectionIds);

    void deleteById(String id);

    boolean existsByUserId(String userId);
}
