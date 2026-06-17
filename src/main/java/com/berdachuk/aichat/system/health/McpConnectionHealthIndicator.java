package com.berdachuk.aichat.system.health;

import com.berdachuk.aichat.mcp.registry.McpConnectionHealthView;
import com.berdachuk.aichat.mcp.registry.McpServerRegistry;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "ai-chat.features.mcp-client", havingValue = "true", matchIfMissing = true)
public class McpConnectionHealthIndicator implements HealthIndicator {

    private final McpServerRegistry registry;

    public McpConnectionHealthIndicator(McpServerRegistry registry) {
        this.registry = registry;
    }

    @Override
    public Health health() {
        McpConnectionHealthView view = registry.healthView();
        Health.Builder builder = "UP".equals(view.status()) ? Health.up() : Health.status(view.status());
        builder.withDetail("connections", view.connections())
                .withDetail("reachable", view.reachable());
        if (view.message() != null) {
            builder.withDetail("message", view.message());
        }
        if (!view.servers().isEmpty()) {
            builder.withDetail("servers", view.servers());
        }
        return builder.build();
    }
}
