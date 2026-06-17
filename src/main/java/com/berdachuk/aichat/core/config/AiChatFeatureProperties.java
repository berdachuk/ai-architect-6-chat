package com.berdachuk.aichat.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai-chat.features")
public record AiChatFeatureProperties(boolean mcpClient) {
}
