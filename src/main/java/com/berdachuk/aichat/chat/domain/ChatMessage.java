package com.berdachuk.aichat.chat.domain;

import java.time.Instant;
import java.util.Map;

public record ChatMessage(
        String id,
        String chatId,
        String role,
        String content,
        int sequenceNumber,
        Integer tokensUsed,
        Instant createdAt,
        Map<String, Object> metadata) {

    public static final String ROLE_USER = "user";
    public static final String ROLE_ASSISTANT = "assistant";
    public static final String ROLE_SYSTEM = "system";
}
