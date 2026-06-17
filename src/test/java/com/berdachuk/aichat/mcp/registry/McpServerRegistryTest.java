package com.berdachuk.aichat.mcp.registry;

import com.berdachuk.aichat.mcp.domain.McpServerInfo;
import com.berdachuk.aichat.mcp.domain.ServerStatus;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class McpServerRegistryTest {

    @Test
    void filtersReachableServersAndBuildsCatalogText() {
        McpServerRegistry registry = new McpServerRegistry();
        registry.register("conn-1", serverInfo("conn-1", "weather", ServerStatus.UP, tool("get_weather", "Current weather")));
        registry.register("conn-2", serverInfo("conn-2", "medical", ServerStatus.DOWN, tool("search", "Search cases")));

        assertThat(registry.getReachableServers(List.of("conn-1", "conn-2"))).hasSize(1);
        assertThat(registry.getToolCallbacks(List.of("conn-1"))).isEmpty();
        assertThat(registry.getToolCatalogText(List.of("conn-1"))).contains("get_weather");
    }

    @Test
    void returnsEmptyWhenNoConnectionsSelected() {
        McpServerRegistry registry = new McpServerRegistry();
        registry.register("conn-1", serverInfo("conn-1", "weather", ServerStatus.UP, tool("get_weather", "Current weather")));

        assertThat(registry.getReachableServers(List.of())).isEmpty();
        assertThat(registry.getToolCallbacks(List.of())).isEmpty();
        assertThat(registry.getToolCatalogText(List.of())).isBlank();
    }

    private static McpServerInfo serverInfo(
            String id, String name, ServerStatus status, McpSchema.Tool tool) {
        return new McpServerInfo(
                id,
                name,
                null,
                name,
                "1.0",
                "http://localhost/sse",
                status,
                status == ServerStatus.DOWN ? "down" : null,
                List.of(tool),
                List.of(),
                List.of());
    }

    private static McpSchema.Tool tool(String name, String description) {
        return McpSchema.Tool.builder().name(name).description(description).build();
    }
}
