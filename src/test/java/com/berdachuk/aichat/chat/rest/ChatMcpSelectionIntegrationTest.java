package com.berdachuk.aichat.chat.rest;

import com.berdachuk.aichat.support.PostgresTestSupport;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class ChatMcpSelectionIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17")
            .withDatabaseName("ai_chat")
            .withUsername("ai_chat")
            .withPassword("ai_chat");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> PostgresTestSupport.jdbcUrlWithSchema(postgres));
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("ai-chat.mcp.bootstrap.enabled", () -> "false");
    }

    @LocalServerPort
    private int port;

    @Autowired
    private JsonMapper jsonMapper;

    @Test
    void chatMcpSelectionRoundTrip() throws Exception {
        RestClient client = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .defaultHeader("X-User-Id", "mcp-chat-user")
                .build();

        String chatJson = client.post()
                .uri("/api/v1/chats")
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"name\":\"MCP Chat\",\"agentId\":\"auto\"}")
                .retrieve()
                .body(String.class);
        String chatId = jsonMapper.readTree(chatJson).get("id").asString();

        String connectionJson = client.post()
                .uri("/api/v1/mcp/connections")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {
                          "name": "test-catalog",
                          "url": "http://localhost:59998/sse",
                          "tools": true,
                          "resources": false,
                          "prompts": false
                        }
                        """)
                .retrieve()
                .body(String.class);
        String connectionId = jsonMapper.readTree(connectionJson).get("id").asString();

        client.put()
                .uri("/api/v1/chats/{chatId}/mcp", chatId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(jsonMapper.writeValueAsString(
                        jsonMapper.createObjectNode().putPOJO("connectionIds", java.util.List.of(connectionId))))
                .retrieve()
                .toBodilessEntity();

        JsonNode selection = jsonMapper.readTree(client.get()
                .uri("/api/v1/chats/{chatId}/mcp", chatId)
                .retrieve()
                .body(String.class));
        assertThat(selection.get("connectionIds")).hasSize(1);
        assertThat(selection.get("connectionIds").get(0).asString()).isEqualTo(connectionId);
    }
}
