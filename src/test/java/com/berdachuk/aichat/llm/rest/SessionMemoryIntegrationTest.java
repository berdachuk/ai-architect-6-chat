package com.berdachuk.aichat.llm.rest;

import com.berdachuk.aichat.llm.rest.dto.SendMessageRequest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
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
class SessionMemoryIntegrationTest {

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

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void secondTurnIncludesPriorSessionContext() {
        RestClient client = restClientBuilder.build();

        String chatId = client.post()
                .uri("http://localhost:" + port + "/api/v1/chats")
                .header("X-User-Id", "memory-user")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CreateChatPayload("Memory test", "auto"))
                .retrieve()
                .body(ChatResponsePayload.class)
                .id();

        streamMessage(client, chatId, "First turn");

        Integer eventCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ai_session_event", Integer.class);
        assertThat(eventCount).isGreaterThanOrEqualTo(2);

        String secondBody = streamMessage(client, chatId, "Second turn");

        assertThat(secondBody).contains("users:2");
    }

    private String streamMessage(RestClient client, String chatId, String content) {
        return client.post()
                .uri("http://localhost:" + port + "/api/v1/chats/" + chatId + "/messages/stream")
                .header("X-User-Id", "memory-user")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .body(new SendMessageRequest(content))
                .retrieve()
                .body(String.class);
    }

    private record CreateChatPayload(String name, String agentId) {
    }

    private record ChatResponsePayload(String id) {
    }
}
