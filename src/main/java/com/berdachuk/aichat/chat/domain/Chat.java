package com.berdachuk.aichat.chat.domain;

import java.time.Instant;
import java.util.Map;

public record Chat(
        String id,
        String userId,
        String name,
        String agentId,
        boolean isDefault,
        Instant createdAt,
        Instant updatedAt,
        Instant lastActivityAt,
        int messageCount) {
}
