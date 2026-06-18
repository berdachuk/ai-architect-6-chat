package com.berdachuk.aichat.system.health;

import com.berdachuk.aichat.mcp.domain.McpServerInfo;
import com.berdachuk.aichat.mcp.domain.ServerStatus;
import com.berdachuk.aichat.mcp.registry.McpServerRegistry;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class McpConnectionHealthIndicatorTest {

    @Test
    void reportsUpWhenNoConnectionsConfigured() {
        McpConnectionHealthIndicator indicator = new McpConnectionHealthIndicator(new McpServerRegistry());

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("connections", 0);
    }

    @Test
    void reportsDegradedWhenAllConnectionsDown() {
        McpServerRegistry registry = new McpServerRegistry();
        registry.register("conn-1", serverInfo("conn-1", "medical", ServerStatus.DOWN));
        McpConnectionHealthIndicator indicator = new McpConnectionHealthIndicator(registry);

        Health health = indicator.health();

        assertThat(health.getStatus().getCode()).isEqualTo("DEGRADED");
        assertThat(health.getDetails()).containsEntry("reachable", 0);
    }

    @Test
    void reportsUpWhenAtLeastOneConnectionReachable() {
        McpServerRegistry registry = new McpServerRegistry();
        registry.register("conn-1", serverInfo("conn-1", "medical", ServerStatus.DOWN));
        registry.register("conn-2", serverInfo("conn-2", "weather", ServerStatus.UP));
        McpConnectionHealthIndicator indicator = new McpConnectionHealthIndicator(registry);

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("reachable", 1);
    }

    private static McpServerInfo serverInfo(String id, String name, ServerStatus status) {
        return new McpServerInfo(
                id,
                name,
                null,
                name,
                "1.0",
                "http://localhost/sse",
                status,
                status == ServerStatus.DOWN ? "unreachable" : null,
                List.of(McpSchema.Tool.builder().name("tool").description("desc").build()),
                List.of(),
                List.of(),
                null);
    }
}
