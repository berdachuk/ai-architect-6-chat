package com.berdachuk.aichat.web;

import com.berdachuk.aichat.support.PostgresTestSupport;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
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
class ChatPageIntegrationTest {

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
    void homeRedirectsToChatPage() {
        RestClient client = restClientBuilder.build();

        String body = client.get()
                .uri("http://localhost:" + port + "/")
                .retrieve()
                .body(String.class);

        assertThat(body)
                .contains("AI Chat")
                .contains("id=\"composer\"")
                .contains("id=\"mcpPanel\"")
                .contains("/js/chat.js")
                .contains("window.AICHAT_CONFIG");
    }

    @Test
    void staticAssetsAreServed() {
        RestClient client = restClientBuilder.build();

        String css = client.get()
                .uri("http://localhost:" + port + "/css/chat.css")
                .retrieve()
                .body(String.class);
        assertThat(css).contains(".message-user");

        String js = client.get()
                .uri("http://localhost:" + port + "/js/chat.js")
                .retrieve()
                .body(String.class);
        assertThat(js).contains("processSseEvent");
        assertThat(js).contains("loadMcpPanel");
    }
}
