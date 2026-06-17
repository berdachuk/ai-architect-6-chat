package com.berdachuk.aichat.llm.support;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import java.util.List;

public class StubChatModel implements ChatModel {

    private final String[] chunks;

    public StubChatModel(String... chunks) {
        this.chunks = chunks;
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        String content = String.join("", chunks);
        return new ChatResponse(List.of(new Generation(new AssistantMessage(content))));
    }

    @Override
    public Flux<ChatResponse> stream(Prompt prompt) {
        return Flux.fromArray(chunks)
                .map(chunk -> new ChatResponse(List.of(new Generation(new AssistantMessage(chunk)))));
    }
}
