package com.berdachuk.aichat.llm.advisor;

import com.berdachuk.aichat.llm.service.ChatStreamActivityPublisher;
import com.berdachuk.aichat.mcp.domain.McpServerInfo;
import com.berdachuk.aichat.mcp.domain.ServerStatus;
import com.berdachuk.aichat.mcp.registry.McpServerRegistry;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class MCPToolAdvisorTest {

    @Test
    void isNoOpWhenNoEnabledConnections() {
        McpServerRegistry registry = new McpServerRegistry();
        registry.register("conn-1", sampleServer("conn-1", "weather", "get_weather"));
        MCPToolAdvisor advisor = new MCPToolAdvisor(registry, mock(ChatStreamActivityPublisher.class));

        ChatClientRequest request = ChatClientRequest.builder()
                .prompt(new Prompt(new UserMessage("Hello")))
                .context(Map.of())
                .build();

        ChatClientRequest advised = advisor.before(request, mock(AdvisorChain.class));
        assertThat(advised.prompt().getInstructions()).hasSize(1);
    }

    @Test
    void injectsCatalogWhenConnectionsEnabled() {
        McpServerRegistry registry = new McpServerRegistry();
        registry.register("conn-1", sampleServer("conn-1", "weather", "get_weather"));
        MCPToolAdvisor advisor = new MCPToolAdvisor(registry, mock(ChatStreamActivityPublisher.class));

        ChatClientRequest request = ChatClientRequest.builder()
                .prompt(new Prompt(new UserMessage("Hello")))
                .context(Map.of(MCPToolAdvisor.ENABLED_CONNECTIONS_CONTEXT_KEY, List.of("conn-1")))
                .build();

        ChatClientRequest advised = advisor.before(request, mock(AdvisorChain.class));
        assertThat(advised.prompt().getSystemMessage().getText()).contains("Available MCP tools");
        assertThat(advised.prompt().getSystemMessage().getText()).contains("get_weather");
    }

    @Test
    void scopesCatalogToEnabledConnectionsOnly() {
        McpServerRegistry registry = new McpServerRegistry();
        registry.register("conn-1", sampleServer("conn-1", "weather", "get_weather"));
        registry.register("conn-2", sampleServer("conn-2", "medical", "search_cases"));
        MCPToolAdvisor advisor = new MCPToolAdvisor(registry, mock(ChatStreamActivityPublisher.class));

        ChatClientRequest request = ChatClientRequest.builder()
                .prompt(new Prompt(new UserMessage("Hello")))
                .context(Map.of(MCPToolAdvisor.ENABLED_CONNECTIONS_CONTEXT_KEY, List.of("conn-1")))
                .build();

        ChatClientRequest advised = advisor.before(request, mock(AdvisorChain.class));
        String systemText = advised.prompt().getSystemMessage().getText();
        assertThat(systemText).contains("get_weather");
        assertThat(systemText).doesNotContain("search_cases");
    }

    private static McpServerInfo sampleServer(String id, String name, String toolName) {
        return new McpServerInfo(
                id,
                name,
                null,
                name,
                "1.0",
                "http://localhost/sse",
                ServerStatus.UP,
                null,
                List.of(McpSchema.Tool.builder().name(toolName).description("Tool").build()),
                List.of(),
                List.of());
    }
}
