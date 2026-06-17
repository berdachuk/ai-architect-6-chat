package com.berdachuk.aichat.mcp.service;

import com.berdachuk.aichat.mcp.domain.McpConnection;
import com.berdachuk.aichat.mcp.domain.McpServerInfo;
import com.berdachuk.aichat.mcp.domain.ServerStatus;
import com.berdachuk.aichat.mcp.registry.McpServerRegistry;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.Duration;
import java.util.List;

@Component
@ConditionalOnProperty(name = "ai-chat.features.mcp-client", havingValue = "true", matchIfMissing = true)
public class McpClientConnector {

    private static final Logger log = LoggerFactory.getLogger(McpClientConnector.class);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);

    private final McpServerRegistry registry;

    public McpClientConnector(McpServerRegistry registry) {
        this.registry = registry;
    }

    public void connect(McpConnection connection) {
        registry.unregister(connection.id());
        try {
            TransportTarget target = resolveTransportTarget(connection.url());
            HttpClientSseClientTransport transport = HttpClientSseClientTransport.builder(target.baseUrl())
                    .sseEndpoint(target.sseEndpoint())
                    .build();
            McpSyncClient client = McpClient.sync(transport)
                    .requestTimeout(REQUEST_TIMEOUT)
                    .build();
            client.initialize();

            McpSchema.Implementation serverInfo = client.getServerInfo();
            List<McpSchema.Tool> tools = connection.toolsEnabled()
                    ? client.listTools().tools()
                    : List.of();
            List<McpSchema.Resource> resources = connection.resourcesEnabled()
                    ? client.listResources().resources()
                    : List.of();
            List<McpSchema.Prompt> prompts = connection.promptsEnabled()
                    ? client.listPrompts().prompts()
                    : List.of();

            registry.register(connection.id(), new McpServerInfo(
                    connection.id(),
                    connection.name(),
                    client,
                    serverInfo != null ? serverInfo.name() : connection.name(),
                    serverInfo != null ? serverInfo.version() : null,
                    connection.url(),
                    ServerStatus.UP,
                    null,
                    tools,
                    resources,
                    prompts));

            log.info(
                    "MCP connection '{}' initialized: {} tools, {} resources, {} prompts",
                    connection.name(),
                    tools.size(),
                    resources.size(),
                    prompts.size());
        } catch (Throwable ex) {
            log.warn("MCP connection '{}' unavailable: {}", connection.name(), ex.getMessage());
            registry.register(connection.id(), new McpServerInfo(
                    connection.id(),
                    connection.name(),
                    null,
                    connection.name(),
                    null,
                    connection.url(),
                    ServerStatus.DOWN,
                    ex.getMessage(),
                    List.of(),
                    List.of(),
                    List.of()));
        }
    }

    public void disconnect(String connectionId) {
        registry.unregister(connectionId);
    }

    static TransportTarget resolveTransportTarget(String url) {
        URI uri = URI.create(url);
        String path = uri.getPath() == null ? "" : uri.getPath();
        if (path.endsWith("/sse")) {
            String basePath = path.substring(0, path.length() - 4);
            String baseUrl = uri.getScheme() + "://" + uri.getAuthority()
                    + (basePath.isEmpty() ? "" : basePath);
            return new TransportTarget(baseUrl, "/sse");
        }
        String baseUrl = uri.getScheme() + "://" + uri.getAuthority()
                + (path.isEmpty() ? "" : path);
        return new TransportTarget(baseUrl, "/sse");
    }

    record TransportTarget(String baseUrl, String sseEndpoint) {
    }
}
