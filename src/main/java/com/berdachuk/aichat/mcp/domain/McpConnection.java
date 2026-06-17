package com.berdachuk.aichat.mcp.domain;

import java.time.Instant;

public record McpConnection(
        String id,
        String name,
        String url,
        boolean toolsEnabled,
        boolean resourcesEnabled,
        boolean promptsEnabled,
        Instant createdAt,
        Instant updatedAt) {
}
