package com.berdachuk.aichat.mcp.rest;

import com.berdachuk.aichat.mcp.rest.dto.CreateMcpConnectionRequest;
import com.berdachuk.aichat.mcp.rest.dto.McpConnectionView;
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
import tools.jackson.databind.json.JsonMapper;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class McpConnectionControllerIntegrationTest {

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
    void mcpConnectionCrudViaRest() throws Exception {
        RestClient client = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .defaultHeader("X-User-Id", "mcp-it-user")
                .build();

        assertThat(client.get().uri("/api/v1/mcp/connections").retrieve().body(McpConnectionView[].class))
                .isEmpty();

        CreateMcpConnectionRequest request = new CreateMcpConnectionRequest(
                "test-server",
                "http://localhost:59999/sse",
                true,
                false,
                false);
        McpConnectionView created = client.post()
                .uri("/api/v1/mcp/connections")
                .contentType(MediaType.APPLICATION_JSON)
                .body(jsonMapper.writeValueAsString(request))
                .retrieve()
                .body(McpConnectionView.class);

        assertThat(created.name()).isEqualTo("test-server");
        assertThat(created.status()).isEqualTo("DOWN");

        List<McpConnectionView> listed = Arrays.asList(client.get()
                .uri("/api/v1/mcp/connections")
                .retrieve()
                .body(McpConnectionView[].class));
        assertThat(listed).hasSize(1);

        client.delete()
                .uri("/api/v1/mcp/connections/{id}", created.id())
                .retrieve()
                .toBodilessEntity();

        assertThat(client.get().uri("/api/v1/mcp/connections").retrieve().body(McpConnectionView[].class))
                .isEmpty();
    }
}
