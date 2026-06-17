package com.berdachuk.aichat.mcp.rest.dto;

import java.time.Instant;

public record McpConnectionView(
        String id,
        String name,
        String url,
        boolean toolsEnabled,
        boolean resourcesEnabled,
        boolean promptsEnabled,
        String status,
        String statusMessage,
        int toolCount,
        String serverName,
        Instant createdAt,
        Instant updatedAt) {
}
