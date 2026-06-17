package com.berdachuk.aichat.core.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@Configuration
@Profile("!test")
@EnableConfigurationProperties(AiChatProperties.class)
public class SpringAIConfig {

    @Bean
    @Lazy
    ChatClient toolCallingChatClient(OpenAiChatModelFactory factory, AiChatProperties props) {
        OpenAiChatModel model = factory.create(props.toolCalling());
        return ChatClient.builder(model).build();
    }
}
