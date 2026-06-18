package com.berdachuk.aichat.mcp.domain;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;

import java.util.List;

public record McpServerInfo(
        String connectionId,
        String connectionName,
        McpSyncClient client,
        String serverName,
        String version,
        String url,
        ServerStatus status,
        String downReason,
        List<McpSchema.Tool> tools,
        List<McpSchema.Resource> resources,
        List<McpSchema.Prompt> prompts,
        String instructions) {

    public McpServerInfo withStatus(ServerStatus newStatus, String reason) {
        return new McpServerInfo(
                connectionId,
                connectionName,
                client,
                serverName,
                version,
                url,
                newStatus,
                reason,
                tools,
                resources,
                prompts,
                instructions);
    }
}
