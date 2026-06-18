package com.berdachuk.aichat.llm.config;

import com.berdachuk.aichat.core.advisor.DateTimeContextAdvisor;
import com.berdachuk.aichat.core.config.AiChatProperties;
import com.berdachuk.aichat.llm.stub.StubChatModel;
import com.berdachuk.aichat.llm.support.SessionAwareStubChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.session.advisor.SessionMemoryAdvisor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("test")
@EnableConfigurationProperties(AiChatProperties.class)
public class TestLlmConfiguration {

    @Bean
    ChatClient primaryChatClient(
            DateTimeContextAdvisor dateTimeContextAdvisor,
            SessionMemoryAdvisor sessionMemoryAdvisor) {
        return ChatClient.builder(new SessionAwareStubChatModel(new StubChatModel("Hello", " world")))
                .defaultAdvisors(dateTimeContextAdvisor, sessionMemoryAdvisor)
                .build();
    }

    @Bean
    ChatClient toolCallingChatClient() {
        return ChatClient.builder(new StubChatModel("tool")).build();
    }
}
