package com.berdachuk.aichat.core.advisor;

import com.berdachuk.aichat.core.util.LlmDateTimeContext;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.core.Ordered;

/**
 * Injects current UTC date/time into every LLM request so models always have temporal context.
 */
public class DateTimeContextAdvisor implements BaseAdvisor {

    @Override
    public String getName() {
        return "dateTimeContextAdvisor";
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public ChatClientRequest before(ChatClientRequest request, AdvisorChain chain) {
        String dateTimeBlock = LlmDateTimeContext.contextBlock();
        return request.mutate()
                .prompt(request.prompt().augmentSystemMessage(systemMessage -> systemMessage.mutate()
                        .text(combine(dateTimeBlock, systemMessage.getText()))
                        .build()))
                .build();
    }

    @Override
    public ChatClientResponse after(ChatClientResponse response, AdvisorChain chain) {
        return response;
    }

    private static String combine(String dateTimeBlock, String existing) {
        if (existing == null || existing.isBlank()) {
            return dateTimeBlock;
        }
        if (existing.startsWith(dateTimeBlock)) {
            return existing;
        }
        return dateTimeBlock + "\n\n" + existing;
    }
}
