package com.berdachuk.aichat.mcp.service;

import com.berdachuk.aichat.core.util.IdGenerator;
import com.berdachuk.aichat.mcp.config.McpBootstrapProperties;
import com.berdachuk.aichat.mcp.domain.McpConnection;
import com.berdachuk.aichat.mcp.repository.McpConnectionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@ConditionalOnProperty(name = "ai-chat.features.mcp-client", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(McpBootstrapProperties.class)
public class McpBootstrapSeeder {

    private static final Logger log = LoggerFactory.getLogger(McpBootstrapSeeder.class);

    private final McpConnectionRepository repository;
    private final McpBootstrapProperties properties;

    public McpBootstrapSeeder(McpConnectionRepository repository, McpBootstrapProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    public void seedIfEmpty() {
        if (!properties.enabled() || !repository.findAll().isEmpty()) {
            return;
        }
        if (properties.name() == null || properties.name().isBlank()
                || properties.url() == null || properties.url().isBlank()) {
            return;
        }
        Instant now = Instant.now();
        McpConnection connection = new McpConnection(
                IdGenerator.generateId(),
                properties.name(),
                properties.url(),
                properties.tools(),
                properties.resources(),
                properties.prompts(),
                now,
                now);
        repository.insert(connection);
        log.info("Seeded default MCP connection '{}'", connection.name());
    }
}
