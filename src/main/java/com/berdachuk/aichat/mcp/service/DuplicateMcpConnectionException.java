package com.berdachuk.aichat.mcp.service;

public class DuplicateMcpConnectionException extends RuntimeException {

    public DuplicateMcpConnectionException(String name) {
        super("MCP connection already exists: " + name);
    }
}
