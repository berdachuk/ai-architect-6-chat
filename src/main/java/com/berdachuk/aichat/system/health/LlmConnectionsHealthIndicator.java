package com.berdachuk.aichat.system.health;

import com.berdachuk.aichat.core.config.AiChatProperties;
import com.berdachuk.aichat.core.config.ChatClientLivenessProbe;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class LlmConnectionsHealthIndicator implements HealthIndicator {

    private static final List<String> ROLES = List.of("chat", "tool-calling");

    private final AiChatProperties props;
    private final ChatClientLivenessProbe probe;

    public LlmConnectionsHealthIndicator(AiChatProperties props, ChatClientLivenessProbe probe) {
        this.props = props;
        this.probe = probe;
    }

    @Override
    public Health health() {
        Map<String, Object> connections = new LinkedHashMap<>();
        boolean allUp = true;

        for (String role : ROLES) {
            AiChatProperties.ModelConfig cfg = configFor(role);
            String status = probe.probe(role, cfg);
            if (!"UP".equals(status)) {
                allUp = false;
            }
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("status", status);
            details.put("model", cfg.model());
            details.put("baseUrl", cfg.baseUrl());
            details.put("provider", cfg.provider());
            if (cfg.temperature() != null) {
                details.put("temperature", cfg.temperature());
            }
            if (cfg.maxTokens() != null) {
                details.put("maxTokens", cfg.maxTokens());
            }
            connections.put(role, details);
        }

        Health.Builder builder = allUp ? Health.up() : Health.down();
        return builder
                .withDetail("connections", connections)
                .withDetail("roles", ROLES)
                .build();
    }

    private AiChatProperties.ModelConfig configFor(String role) {
        return switch (role) {
            case "chat" -> props.chat();
            case "tool-calling" -> props.toolCalling();
            default -> throw new IllegalArgumentException("Unknown LLM role: " + role);
        };
    }
}
