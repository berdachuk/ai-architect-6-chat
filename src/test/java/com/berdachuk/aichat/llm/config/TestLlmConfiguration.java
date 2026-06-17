package com.berdachuk.aichat.llm.config;

import com.berdachuk.aichat.core.config.AiChatProperties;
import com.berdachuk.aichat.llm.support.StubChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("test")
@EnableConfigurationProperties(AiChatProperties.class)
public class TestLlmConfiguration {

    @Bean
    ChatClient primaryChatClient() {
        return ChatClient.builder(new StubChatModel("Hello", " world")).build();
    }

    @Bean
    ChatClient toolCallingChatClient() {
        return ChatClient.builder(new StubChatModel("tool")).build();
    }
}
