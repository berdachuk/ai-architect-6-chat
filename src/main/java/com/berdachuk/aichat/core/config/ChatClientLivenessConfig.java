package com.berdachuk.aichat.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
@Profile("!test")
public class ChatClientLivenessConfig {

    @Bean
    public HttpClient llmProbeHttpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
    }
}
