package com.berdachuk.aichat.llm.rest;

import com.berdachuk.aichat.llm.rest.dto.SendMessageRequest;
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
import com.berdachuk.aichat.support.PostgresTestSupport;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class ChatStreamControllerIntegrationTest {

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
    void streamEndpointReturnsTokenAndDoneEvents() throws Exception {
        RestClient client = restClientBuilder.build();

        String chatId = client.post()
                .uri("http://localhost:" + port + "/api/v1/chats")
                .header("X-User-Id", "stream-user")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CreateChatPayload("Stream test", "auto"))
                .retrieve()
                .body(ChatResponsePayload.class)
                .id();

        String body = client.post()
                .uri("http://localhost:" + port + "/api/v1/chats/" + chatId + "/messages/stream")
                .header("X-User-Id", "stream-user")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .body(new SendMessageRequest("Hi"))
                .retrieve()
                .body(String.class);

        assertThat(body).contains("event:token");
        assertThat(body).contains("event:done");
        assertThat(body).contains("Hello");
        assertThat(body).contains("world");

        var history = client.get()
                .uri("http://localhost:" + port + "/api/v1/chats/" + chatId + "/history")
                .header("X-User-Id", "stream-user")
                .retrieve()
                .body(HistoryMessage[].class);

        assertThat(history).hasSize(2);
        assertThat(history[0].role()).isEqualTo("user");
        assertThat(history[1].role()).isEqualTo("assistant");
        assertThat(history[1].content()).isEqualTo("Hello world");
    }

    private record CreateChatPayload(String name, String agentId) {
    }

    private record ChatResponsePayload(String id) {
    }

    private record HistoryMessage(String role, String content) {
    }
}
