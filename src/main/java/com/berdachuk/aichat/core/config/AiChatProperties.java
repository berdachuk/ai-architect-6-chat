package com.berdachuk.aichat.core.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "spring.ai.custom")
@Validated
public record AiChatProperties(
        @Valid ModelConfig chat,
        @Valid ModelConfig chatAlt,
        @Valid ModelConfig toolCalling) {

    public record ModelConfig(
            @NotBlank String provider,
            @NotBlank String baseUrl,
            String apiKey,
            @NotBlank String model,
            Double temperature,
            Integer maxTokens) {
    }
}
