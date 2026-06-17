package com.berdachuk.aichat.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai-chat.security")
public record AiChatSecurityProperties(
        boolean oauth2Enabled,
        boolean oauth2LoginEnabled,
        String jwtUserClaim) {

    public AiChatSecurityProperties {
        if (jwtUserClaim == null || jwtUserClaim.isBlank()) {
            jwtUserClaim = "sub";
        }
    }
}
