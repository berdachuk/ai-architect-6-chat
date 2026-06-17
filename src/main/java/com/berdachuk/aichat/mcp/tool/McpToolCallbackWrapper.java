package com.berdachuk.aichat.mcp.tool;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.ai.mcp.SyncMcpToolCallback;
import org.springframework.ai.tool.ToolCallback;

/**
 * MCP tool exposed as a Spring AI {@link ToolCallback} with server attribution in descriptions.
 */
public class McpToolCallbackWrapper implements ToolCallback {

    private final SyncMcpToolCallback delegate;

    public McpToolCallbackWrapper(String serverName, McpSyncClient client, McpSchema.Tool tool) {
        this.delegate = SyncMcpToolCallback.builder()
                .mcpClient(client)
                .tool(tool)
                .prefixedToolName(serverName)
                .build();
    }

    @Override
    public org.springframework.ai.tool.definition.ToolDefinition getToolDefinition() {
        return delegate.getToolDefinition();
    }

    @Override
    public String call(String toolInput) {
        return delegate.call(toolInput);
    }

    @Override
    public String call(String toolInput, org.springframework.ai.chat.model.ToolContext toolContext) {
        return delegate.call(toolInput, toolContext);
    }
}
