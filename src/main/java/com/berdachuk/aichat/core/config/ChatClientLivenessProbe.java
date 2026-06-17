package com.berdachuk.aichat.core.config;

import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Lightweight liveness probe for configured LLM endpoints. Performs a HEAD request
 * to the base URL (e.g. Ollama) to verify reachability. Avoids real chat calls
 * so health checks stay cheap and do not consume model tokens.
 */
@Component
public class ChatClientLivenessProbe {

    private final HttpClient httpClient;

    public ChatClientLivenessProbe(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public String probe(String role, AiChatProperties.ModelConfig cfg) {
        if (cfg == null || cfg.baseUrl() == null || cfg.baseUrl().isBlank()) {
            return "DOWN";
        }
        String url = cfg.baseUrl().replaceAll("/v1/?$", "");
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .timeout(Duration.ofSeconds(2))
                    .build();
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            int code = response.statusCode();
            return code < 500 ? "UP" : "DOWN";
        } catch (Exception e) {
            return "DOWN";
        }
    }
}
