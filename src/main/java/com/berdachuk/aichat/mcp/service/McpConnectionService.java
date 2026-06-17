package com.berdachuk.aichat.mcp.service;

import com.berdachuk.aichat.mcp.domain.McpConnection;
import com.berdachuk.aichat.mcp.domain.McpServerInfo;
import com.berdachuk.aichat.mcp.domain.ServerStatus;
import com.berdachuk.aichat.mcp.registry.McpServerRegistry;
import com.berdachuk.aichat.mcp.repository.McpConnectionRepository;
import com.berdachuk.aichat.mcp.rest.dto.CreateMcpConnectionRequest;
import com.berdachuk.aichat.mcp.rest.dto.McpConnectionView;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
@ConditionalOnProperty(name = "ai-chat.features.mcp-client", havingValue = "true", matchIfMissing = true)
public class McpConnectionService {

    private final McpConnectionRepository repository;
    private final McpClientConnector connector;
    private final McpServerRegistry registry;
    private final ApplicationEventPublisher eventPublisher;
    private final McpBootstrapSeeder bootstrapSeeder;

    public McpConnectionService(
            McpConnectionRepository repository,
            McpClientConnector connector,
            McpServerRegistry registry,
            ApplicationEventPublisher eventPublisher,
            McpBootstrapSeeder bootstrapSeeder) {
        this.repository = repository;
        this.connector = connector;
        this.registry = registry;
        this.eventPublisher = eventPublisher;
        this.bootstrapSeeder = bootstrapSeeder;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void loadCatalogOnStartup() {
        bootstrapSeeder.seedIfEmpty();
        repository.findAll().forEach(connector::connect);
    }

    @Transactional(readOnly = true)
    public List<McpConnectionView> listConnections() {
        return repository.findAll().stream().map(this::toView).toList();
    }

    public McpConnectionView createConnection(CreateMcpConnectionRequest request) {
        if (repository.existsByName(request.name())) {
            throw new DuplicateMcpConnectionException(request.name());
        }
        Instant now = Instant.now();
        McpConnection connection = new McpConnection(
                com.berdachuk.aichat.core.util.IdGenerator.generateId(),
                request.name(),
                request.url(),
                request.tools(),
                request.resources(),
                request.prompts(),
                now,
                now);
        repository.insert(connection);
        eventPublisher.publishEvent(new McpConnectionRegisteredEvent(connection));
        return toView(connection);
    }

    public void deleteConnection(String connectionId) {
        McpConnection connection = repository.findById(connectionId)
                .orElseThrow(() -> new McpConnectionNotFoundException(connectionId));
        connector.disconnect(connection.id());
        repository.deleteById(connection.id());
    }

    private McpConnectionView toView(McpConnection connection) {
        Optional<McpServerInfo> runtime = registry.findById(connection.id());
        ServerStatus status = runtime.map(McpServerInfo::status).orElse(ServerStatus.DOWN);
        String downReason = runtime.map(McpServerInfo::downReason).orElse("Not connected");
        int toolCount = runtime.map(info -> info.tools().size()).orElse(0);
        String serverName = runtime.map(McpServerInfo::serverName).orElse(null);
        return new McpConnectionView(
                connection.id(),
                connection.name(),
                connection.url(),
                connection.toolsEnabled(),
                connection.resourcesEnabled(),
                connection.promptsEnabled(),
                status.name(),
                downReason,
                toolCount,
                serverName,
                connection.createdAt(),
                connection.updatedAt());
    }
}
