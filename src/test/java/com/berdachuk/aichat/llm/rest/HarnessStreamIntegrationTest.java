package com.berdachuk.aichat.llm.rest;

import com.berdachuk.aichat.llm.rest.dto.SendMessageRequest;
import com.berdachuk.aichat.support.PostgresTestSupport;
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
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class HarnessStreamIntegrationTest {

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
    void streamEndpointEmitsHarnessProgressEvents() {
        RestClient client = restClientBuilder.build();

        String chatId = client.post()
                .uri("http://localhost:" + port + "/api/v1/chats")
                .header("X-User-Id", "harness-user")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CreateChatPayload("Harness test", "auto"))
                .retrieve()
                .body(ChatResponsePayload.class)
                .id();

        String body = client.post()
                .uri("http://localhost:" + port + "/api/v1/chats/" + chatId + "/messages/stream")
                .header("X-User-Id", "harness-user")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .body(new SendMessageRequest("Explain harness"))
                .retrieve()
                .body(String.class);

        assertThat(body).contains("event:agent");
        assertThat(body).contains("agent_start");
        assertThat(body).contains("event:pipeline_stage");
        assertThat(body).contains("PLANNING");
        assertThat(body).contains("DONE");
        assertThat(body).contains("event:activity");
        assertThat(body).contains("todo_update");
        assertThat(body).contains("event:token");
        assertThat(body).contains("event:done");
        assertThat(body).contains("agent_done");

        Integer runs = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ai_chat.harness_workflow_run", Integer.class);
        assertThat(runs).isEqualTo(1);
    }

    private record CreateChatPayload(String name, String agentId) {
    }

    private record ChatResponsePayload(String id) {
    }
}
