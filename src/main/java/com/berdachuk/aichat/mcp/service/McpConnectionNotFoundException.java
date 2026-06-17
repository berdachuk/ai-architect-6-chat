package com.berdachuk.aichat.mcp.service;

public class McpConnectionNotFoundException extends RuntimeException {

    public McpConnectionNotFoundException(String connectionId) {
        super("MCP connection not found: " + connectionId);
    }
}
