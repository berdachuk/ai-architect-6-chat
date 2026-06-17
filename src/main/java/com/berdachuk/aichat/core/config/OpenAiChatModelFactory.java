package com.berdachuk.aichat.core.config;

import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Component;

@Component
public class OpenAiChatModelFactory {

    public OpenAiChatModel create(AiChatProperties.ModelConfig config) {
        OpenAiChatOptions.Builder options = OpenAiChatOptions.builder()
                .baseUrl(normalizeBaseUrl(config.baseUrl()))
                .apiKey(config.apiKey() != null ? config.apiKey() : "ollama")
                .model(config.model());
        if (config.temperature() != null) {
            options.temperature(config.temperature());
        }
        if (config.maxTokens() != null) {
            options.maxTokens(config.maxTokens());
        }
        return OpenAiChatModel.builder()
                .options(options.build())
                .build();
    }

    String normalizeBaseUrl(String url) {
        if (url == null || url.isBlank()) {
            return "http://localhost:11434/v1";
        }
        String trimmed = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
        if (!trimmed.endsWith("/v1")) {
            trimmed = trimmed + "/v1";
        }
        return trimmed;
    }
}
