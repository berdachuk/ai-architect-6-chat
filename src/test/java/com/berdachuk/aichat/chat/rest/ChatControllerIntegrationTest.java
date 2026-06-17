package com.berdachuk.aichat.chat.rest;

import com.berdachuk.aichat.test.client.ApiClient;
import com.berdachuk.aichat.test.client.ApiException;
import com.berdachuk.aichat.test.client.api.ChatsApi;
import com.berdachuk.aichat.test.client.model.Chat;
import com.berdachuk.aichat.test.client.model.CreateChatRequest;
import com.berdachuk.aichat.test.client.model.RenameChatRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import com.berdachuk.aichat.support.PostgresTestSupport;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class ChatControllerIntegrationTest {

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

    private ChatsApi chatsApi;

    @BeforeEach
    void setUpClient() {
        ApiClient apiClient = new ApiClient();
        apiClient.updateBaseUri("http://localhost:" + port);
        apiClient.setRequestInterceptor(builder -> builder.header("X-User-Id", "it-user"));
        chatsApi = new ChatsApi(apiClient);
    }

    @Test
    void chatCrudAndDefaultRecreationViaGeneratedClient() throws ApiException {
        List<Chat> initial = chatsApi.listChats();
        assertThat(initial).isEmpty();

        Chat created = chatsApi.createChat(new CreateChatRequest().name("First").agentId("auto"));
        assertThat(created.getName()).isEqualTo("First");
        assertThat(created.getIsDefault()).isTrue();

        Chat second = chatsApi.createChat(new CreateChatRequest().name("Second").agentId("auto"));
        assertThat(second.getIsDefault()).isFalse();

        List<Chat> listed = chatsApi.listChats();
        assertThat(listed).hasSize(2);

        chatsApi.renameChat(created.getId(), new RenameChatRequest().name("Renamed"));
        chatsApi.deleteChat(created.getId());

        List<Chat> afterDeleteOne = chatsApi.listChats();
        assertThat(afterDeleteOne).hasSize(1);
        assertThat(afterDeleteOne.getFirst().getId()).isEqualTo(second.getId());

        chatsApi.deleteChat(second.getId());

        List<Chat> afterDeleteAll = chatsApi.listChats();
        assertThat(afterDeleteAll).hasSize(1);
        assertThat(afterDeleteAll.getFirst().getIsDefault()).isTrue();
        assertThat(afterDeleteAll.getFirst().getName()).isEqualTo("New Chat");

        assertThat(chatsApi.getChatHistory(afterDeleteAll.getFirst().getId(), 50, 0)).isEmpty();
    }
}
