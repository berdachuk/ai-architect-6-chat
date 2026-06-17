package com.berdachuk.aichat.mcp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai-chat.mcp.bootstrap")
public record McpBootstrapProperties(
        boolean enabled,
        String name,
        String url,
        boolean tools,
        boolean resources,
        boolean prompts) {
}
