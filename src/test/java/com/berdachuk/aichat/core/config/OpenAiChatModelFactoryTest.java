package com.berdachuk.aichat.core.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OpenAiChatModelFactoryTest {

    private final OpenAiChatModelFactory factory = new OpenAiChatModelFactory();

    @Test
    void normalizeBaseUrl_appendsV1WhenMissing() {
        assertThat(factory.normalizeBaseUrl("http://localhost:11434")).isEqualTo("http://localhost:11434/v1");
        assertThat(factory.normalizeBaseUrl("http://localhost:11434/")).isEqualTo("http://localhost:11434/v1");
    }

    @Test
    void normalizeBaseUrl_keepsExistingV1Suffix() {
        assertThat(factory.normalizeBaseUrl("http://localhost:11434/v1")).isEqualTo("http://localhost:11434/v1");
    }

    @Test
    void normalizeBaseUrl_defaultsWhenBlank() {
        assertThat(factory.normalizeBaseUrl(null)).isEqualTo("http://localhost:11434/v1");
        assertThat(factory.normalizeBaseUrl("  ")).isEqualTo("http://localhost:11434/v1");
    }
}
