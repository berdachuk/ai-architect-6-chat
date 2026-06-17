package com.berdachuk.aichat.llm.support;

import com.berdachuk.aichat.llm.stub.StubChatModel;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * Delegates streaming to a stub but reports how many user messages are in the prompt
 * (used to verify SessionMemoryAdvisor injects prior turns).
 */
public class SessionAwareStubChatModel implements ChatModel {

    private final ChatModel delegate;

    public SessionAwareStubChatModel(ChatModel delegate) {
        this.delegate = delegate;
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        int userMessages = prompt.getUserMessages().size();
        if (userMessages > 1) {
            return historyResponse(userMessages);
        }
        return delegate.call(prompt);
    }

    @Override
    public Flux<ChatResponse> stream(Prompt prompt) {
        int userMessages = prompt.getUserMessages().size();
        if (userMessages > 1) {
            String content = "users:" + userMessages;
            return Flux.just(new ChatResponse(List.of(new Generation(new AssistantMessage(content)))));
        }
        return delegate.stream(prompt);
    }

    private static ChatResponse historyResponse(int userMessages) {
        String content = "users:" + userMessages;
        return new ChatResponse(List.of(new Generation(new AssistantMessage(content))));
    }
}
