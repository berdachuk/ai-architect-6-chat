package com.berdachuk.aichat.llm.advisor;

import com.berdachuk.aichat.llm.service.ChatStreamActivityPublisher;
import com.berdachuk.aichat.llm.tool.ActivityReportingToolCallback;
import com.berdachuk.aichat.mcp.registry.McpServerRegistry;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.session.advisor.SessionMemoryAdvisor;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.model.tool.DefaultToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Set;

@Component
public class MCPToolAdvisor implements BaseAdvisor {

    public static final String ENABLED_CONNECTIONS_CONTEXT_KEY = "mcp_enabled_connections";

    private final McpServerRegistry registry;
    private final ChatStreamActivityPublisher activityPublisher;

    public MCPToolAdvisor(McpServerRegistry registry, ChatStreamActivityPublisher activityPublisher) {
        this.registry = registry;
        this.activityPublisher = activityPublisher;
    }

    @Override
    public String getName() {
        return "mcpToolAdvisor";
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }

    @Override
    public ChatClientRequest before(ChatClientRequest request, AdvisorChain chain) {
        Collection<String> enabledConnections = resolveEnabledConnections(request);
        if (enabledConnections.isEmpty()) {
            return request;
        }

        String catalog = registry.getToolCatalogText(enabledConnections);
        List<ToolCallback> toolCallbacks = wrapForActivity(
                registry.getToolCallbacks(enabledConnections), resolveSessionId(request));
        if (catalog.isBlank() && toolCallbacks.isEmpty()) {
            return request;
        }

        ChatClientRequest updated = request;
        if (!catalog.isBlank()) {
            updated = updated.mutate()
                    .prompt(updated.prompt().augmentSystemMessage(systemMessage -> systemMessage.mutate()
                            .text(combineCatalog(systemMessage.getText(), catalog))
                            .build()))
                    .build();
        }

        if (toolCallbacks.isEmpty()) {
            return updated;
        }

        ChatOptions options = updated.prompt().getOptions();
        DefaultToolCallingChatOptions.Builder<?> toolOptions = DefaultToolCallingChatOptions.builder();
        if (options != null) {
            toolOptions.combineWith(options.mutate());
        }
        if (options instanceof ToolCallingChatOptions toolCallingChatOptions) {
            toolOptions.toolCallbacks(ToolCallingChatOptions.mergeToolCallbacks(
                    toolCallingChatOptions.getToolCallbacks(), toolCallbacks));
        } else {
            toolOptions.toolCallbacks(toolCallbacks);
        }

        return updated.mutate()
                .prompt(updated.prompt().mutate().chatOptions(toolOptions.build()).build())
                .build();
    }

    @Override
    public ChatClientResponse after(ChatClientResponse response, AdvisorChain chain) {
        return response;
    }

    @SuppressWarnings("unchecked")
    private static Collection<String> resolveEnabledConnections(ChatClientRequest request) {
        Object value = request.context().get(ENABLED_CONNECTIONS_CONTEXT_KEY);
        if (value instanceof Collection<?> collection) {
            return collection.stream().map(Object::toString).toList();
        }
        if (value instanceof String single) {
            return Set.of(single);
        }
        return List.of();
    }

    private List<ToolCallback> wrapForActivity(List<ToolCallback> callbacks, String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return callbacks;
        }
        return callbacks.stream()
                .<ToolCallback>map(callback -> new ActivityReportingToolCallback(callback, activityPublisher, sessionId))
                .toList();
    }

    private static String resolveSessionId(ChatClientRequest request) {
        Object value = request.context().get(SessionMemoryAdvisor.SESSION_ID_CONTEXT_KEY);
        return value == null ? null : value.toString();
    }

    private static String combineCatalog(String existing, String catalog) {
        if (catalog == null || catalog.isBlank()) {
            return existing;
        }
        String block = "Available MCP tools:\n" + catalog;
        if (existing == null || existing.isBlank()) {
            return block;
        }
        if (existing.contains(block)) {
            return existing;
        }
        return existing + "\n\n" + block;
    }
}
