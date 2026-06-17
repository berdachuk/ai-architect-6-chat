package com.berdachuk.aichat.core.advisor;

import com.berdachuk.aichat.core.util.LlmDateTimeContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.prompt.Prompt;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class DateTimeContextAdvisorTest {

    @AfterEach
    void resetClock() {
        LlmDateTimeContext.resetClock();
    }

    @Test
    void before_injectsUtcDateTimeIntoSystemMessage() {
        LlmDateTimeContext.setClock(Clock.fixed(Instant.parse("2026-06-17T12:00:00Z"), ZoneOffset.UTC));

        DateTimeContextAdvisor advisor = new DateTimeContextAdvisor();
        ChatClientRequest request = ChatClientRequest.builder()
                .prompt(new Prompt("Hello"))
                .build();

        ChatClientRequest augmented = advisor.before(request, mock(AdvisorChain.class));

        assertThat(augmented.prompt().getSystemMessage().getText())
                .contains("Current date and time (UTC): 2026-06-17T12:00:00Z");
    }
}
