package com.berdachuk.aichat.mcp.registry;

import java.util.Map;

public record McpConnectionHealthView(
        String status,
        int connections,
        int reachable,
        String message,
        Map<String, Map<String, Object>> servers) {

    public static McpConnectionHealthView empty() {
        return new McpConnectionHealthView("UP", 0, 0, "No MCP connections configured", Map.of());
    }
}
