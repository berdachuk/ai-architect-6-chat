package com.berdachuk.aichat.mcp.registry;

import com.berdachuk.aichat.mcp.domain.McpServerInfo;
import com.berdachuk.aichat.mcp.domain.ServerStatus;
import com.berdachuk.aichat.mcp.tool.McpToolCallbackWrapper;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class McpServerRegistry {

    private final Map<String, McpServerInfo> servers = new ConcurrentHashMap<>();

    public void register(String connectionId, McpServerInfo info) {
        servers.put(connectionId, info);
    }

    public void unregister(String connectionId) {
        Optional.ofNullable(servers.remove(connectionId))
                .map(McpServerInfo::client)
                .ifPresent(client -> {
                    try {
                        client.closeGracefully();
                    } catch (RuntimeException ignored) {
                        client.close();
                    }
                });
    }

    public void markDown(String connectionId, String reason) {
        servers.computeIfPresent(connectionId, (id, info) -> info.withStatus(ServerStatus.DOWN, reason));
    }

    public Optional<McpServerInfo> findById(String connectionId) {
        return Optional.ofNullable(servers.get(connectionId));
    }

    public List<McpServerInfo> getAllServers() {
        return List.copyOf(servers.values());
    }

    public McpConnectionHealthView healthView() {
        List<McpServerInfo> allServers = getAllServers();
        if (allServers.isEmpty()) {
            return McpConnectionHealthView.empty();
        }

        long reachable = allServers.stream().filter(server -> server.status() == ServerStatus.UP).count();
        Map<String, Map<String, Object>> serverDetails = new LinkedHashMap<>();
        allServers.forEach(server -> serverDetails.put(server.connectionName(), Map.of(
                "status", server.status().name(),
                "url", server.url(),
                "tools", server.tools().size(),
                "reason", server.downReason() == null ? "" : server.downReason())));

        String status = reachable > 0 ? "UP" : "DEGRADED";
        return new McpConnectionHealthView(status, allServers.size(), (int) reachable, null, serverDetails);
    }

    public List<McpServerInfo> getReachableServers() {
        return servers.values().stream()
                .filter(server -> server.status() == ServerStatus.UP)
                .toList();
    }

    public List<McpServerInfo> getReachableServers(Collection<String> connectionIds) {
        if (connectionIds == null || connectionIds.isEmpty()) {
            return List.of();
        }
        Set<String> selected = Set.copyOf(connectionIds);
        return getReachableServers().stream()
                .filter(server -> selected.contains(server.connectionId()))
                .toList();
    }

    public List<ToolCallback> getToolCallbacks(Collection<String> connectionIds) {
        List<ToolCallback> callbacks = new ArrayList<>();
        getReachableServers(connectionIds).stream()
                .filter(server -> server.client() != null)
                .forEach(server -> server.tools().forEach(tool ->
                        callbacks.add(new McpToolCallbackWrapper(server.connectionName(), server.client(), tool))));
        return callbacks;
    }

    public String getToolCatalogText(Collection<String> connectionIds) {
        return getReachableServers(connectionIds).stream()
                .map(this::formatServerCatalog)
                .collect(Collectors.joining("\n"));
    }

    private String formatServerCatalog(McpServerInfo server) {
        StringBuilder builder = new StringBuilder();
        builder.append("### ").append(server.connectionName());
        if (server.serverName() != null && !server.serverName().isBlank()) {
            builder.append(" (").append(server.serverName()).append(")");
        }
        builder.append("\n");
        server.tools().forEach(tool -> builder.append("- **")
                .append(tool.name())
                .append("**: ")
                .append(tool.description() == null ? "" : tool.description())
                .append("\n"));
        return builder.toString().trim();
    }
}
