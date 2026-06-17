package com.berdachuk.aichat.smoke;

import com.berdachuk.aichat.llm.rest.dto.SendMessageRequest;
import com.berdachuk.aichat.support.PostgresTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Automates REST-verifiable items from docs/04-testing.md §7 manual smoke checklist.
 */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class SmokeChecklistIntegrationTest {

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
    }

    @LocalServerPort
    private int port;

    @Autowired
    private RestClient.Builder restClientBuilder;

    @Test
    @DisplayName("smoke: health endpoint returns UP")
    void healthEndpointReturnsUp() {
        String body = client().get()
                .uri(baseUrl() + "/actuator/health")
                .retrieve()
                .body(String.class);

        assertThat(body).contains("\"status\":\"UP\"");
    }

    @Test
    @DisplayName("smoke: home opens default chat page")
    void homeOpensDefaultChatPage() {
        String body = client().get()
                .uri(baseUrl() + "/")
                .retrieve()
                .body(String.class);

        assertThat(body)
                .contains("AI Chat")
                .contains("id=\"composer\"")
                .contains("New Chat");
    }

    @Test
    @DisplayName("smoke: chat CRUD and default recreation via REST")
    void chatCrudAndDefaultRecreation() {
        RestClient client = clientForUser("smoke-user");

        assertThat(client.get().uri(baseUrl() + "/api/v1/chats").retrieve().body(String.class))
                .isEqualTo("[]");

        String firstId = createChat(client, baseUrl(), "First");
        String secondId = createChat(client, baseUrl(), "Second");

        String listed = client.get().uri(baseUrl() + "/api/v1/chats").retrieve().body(String.class);
        assertThat(listed).contains("First", "Second");

        String history = client.get()
                .uri(baseUrl() + "/api/v1/chats/" + secondId + "/history")
                .retrieve()
                .body(String.class);
        assertThat(history).isEqualTo("[]");

        client.delete().uri(baseUrl() + "/api/v1/chats/" + firstId).retrieve().toBodilessEntity();
        client.delete().uri(baseUrl() + "/api/v1/chats/" + secondId).retrieve().toBodilessEntity();

        String afterDelete = client.get().uri(baseUrl() + "/api/v1/chats").retrieve().body(String.class);
        assertThat(afterDelete).contains("New Chat").contains("\"isDefault\":true");
    }

    @Test
    @DisplayName("smoke: streaming works without MCP")
    void streamingWorksWithoutMcp() {
        RestClient client = clientForUser("smoke-stream");
        String chatId = createChat(client, baseUrl(), "Stream");

        String body = client.post()
                .uri(baseUrl() + "/api/v1/chats/" + chatId + "/messages/stream")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .body(new SendMessageRequest("hello"))
                .retrieve()
                .body(String.class);

        assertThat(body).contains("event:token").contains("event:done");
    }

    private RestClient client() {
        return restClientBuilder.build();
    }

    private RestClient clientForUser(String userId) {
        return restClientBuilder
                .defaultHeader("X-User-Id", userId)
                .build();
    }

    private String baseUrl() {
        return "http://localhost:" + port;
    }

    private static String createChat(RestClient client, String baseUrl, String name) {
        return client.post()
                .uri(baseUrl + "/api/v1/chats")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CreateChatPayload(name, "auto"))
                .retrieve()
                .body(ChatResponse.class)
                .id();
    }

    private record CreateChatPayload(String name, String agentId) {
    }

    private record ChatResponse(String id) {
    }
}
