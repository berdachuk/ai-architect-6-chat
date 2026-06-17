package com.berdachuk.aichat.llm.config;

import com.berdachuk.aichat.core.advisor.DateTimeContextAdvisor;
import com.berdachuk.aichat.core.config.AiChatProperties;
import com.berdachuk.aichat.core.config.OpenAiChatModelFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.session.advisor.SessionMemoryAdvisor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!test")
@EnableConfigurationProperties(AiChatProperties.class)
public class LlmChatClientConfiguration {

    @Bean
    @Lazy
    ChatClient primaryChatClient(
            OpenAiChatModelFactory factory,
            AiChatProperties props,
            DateTimeContextAdvisor dateTimeContextAdvisor,
            SessionMemoryAdvisor sessionMemoryAdvisor) {
        OpenAiChatModel model = factory.create(props.chat());
        return ChatClient.builder(model)
                .defaultAdvisors(dateTimeContextAdvisor, sessionMemoryAdvisor)
                .build();
    }
}
