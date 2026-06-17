package com.berdachuk.aichat.llm.service.impl;

import com.berdachuk.aichat.chat.domain.ChatMessage;
import com.berdachuk.aichat.chat.service.ChatService;
import com.berdachuk.aichat.llm.service.ChatAssistantService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.session.advisor.SessionMemoryAdvisor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.Map;

@Service
public class ChatAssistantServiceImpl implements ChatAssistantService {

    private static final long SSE_TIMEOUT_MS = 120_000L;

    private final ChatService chatService;
    private final ChatClient chatClient;

    public ChatAssistantServiceImpl(
            ChatService chatService,
            @Qualifier("primaryChatClient") ChatClient chatClient) {
        this.chatService = chatService;
        this.chatClient = chatClient;
    }

    @Override
    public String processMessage(String userId, String chatId, String userMessage) {
        chatService.requireOwnedChat(userId, chatId);
        chatService.appendUserMessage(chatId, userMessage);
        try {
            String content = chatClient.prompt()
                    .user(userMessage)
                    .advisors(sessionAdvisors(userId, chatId))
                    .call()
                    .content();
            chatService.appendAssistantMessage(chatId, content, estimateTokens(content), Map.of());
            return content;
        } catch (RuntimeException ex) {
            throw new LlmUnavailableException("LLM request failed", ex);
        }
    }

    @Override
    public SseEmitter streamMessage(String userId, String chatId, String userMessage) {
        chatService.requireOwnedChat(userId, chatId);
        chatService.appendUserMessage(chatId, userMessage);

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        StringBuilder fullResponse = new StringBuilder();

        Flux<ChatResponse> flux;
        try {
            flux = chatClient.prompt()
                    .user(userMessage)
                    .advisors(sessionAdvisors(userId, chatId))
                    .stream()
                    .chatResponse();
        } catch (RuntimeException ex) {
            sendErrorAndComplete(emitter, ex);
            return emitter;
        }

        flux.subscribe(
                response -> {
                    String token = extractToken(response);
                    if (token != null && !token.isEmpty()) {
                        fullResponse.append(token);
                        sendSseEvent(emitter, "token", Map.of("t", token));
                    }
                },
                error -> sendErrorAndComplete(emitter, error),
                () -> completeStream(emitter, chatId, fullResponse.toString()));

        return emitter;
    }

    private void completeStream(SseEmitter emitter, String chatId, String content) {
        ChatMessage assistant = chatService.appendAssistantMessage(
                chatId, content, estimateTokens(content), Map.of());
        sendSseEvent(emitter, "done", Map.of("id", assistant.id(), "content", content));
        emitter.complete();
    }

    private void sendErrorAndComplete(SseEmitter emitter, Throwable error) {
        sendSseEvent(emitter, "error", Map.of("message", "LLM unavailable"));
        emitter.complete();
    }

    private static String extractToken(ChatResponse response) {
        if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
            return null;
        }
        return response.getResult().getOutput().getText();
    }

    private static void sendSseEvent(SseEmitter emitter, String event, Object data) {
        try {
            emitter.send(SseEmitter.event().name(event).data(data, MediaType.APPLICATION_JSON));
        } catch (IOException ex) {
            emitter.completeWithError(ex);
        }
    }

    private static int estimateTokens(String content) {
        return content == null || content.isEmpty() ? 0 : Math.max(1, content.length() / 4);
    }

    private static java.util.function.Consumer<ChatClient.AdvisorSpec> sessionAdvisors(String userId, String chatId) {
        String sessionId = userId + "-" + chatId;
        return spec -> spec
                .param(SessionMemoryAdvisor.SESSION_ID_CONTEXT_KEY, sessionId)
                .param(SessionMemoryAdvisor.USER_ID_CONTEXT_KEY, userId);
    }
}
