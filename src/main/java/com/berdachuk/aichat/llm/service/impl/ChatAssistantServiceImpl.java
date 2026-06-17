package com.berdachuk.aichat.llm.service.impl;

import com.berdachuk.aichat.chat.domain.ChatMessage;
import com.berdachuk.aichat.chat.service.ChatService;
import com.berdachuk.aichat.llm.advisor.MCPToolAdvisor;
import com.berdachuk.aichat.llm.harness.ChatWorkflowEngine;
import com.berdachuk.aichat.llm.harness.domain.HarnessResult;
import com.berdachuk.aichat.llm.service.ChatAssistantService;
import com.berdachuk.aichat.llm.service.ChatStreamActivityPublisher;
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
    private static final String AGENT_ID = "chat-orchestrator";

    private final ChatService chatService;
    private final ChatClient chatClient;
    private final ChatWorkflowEngine workflowEngine;
    private final ChatStreamActivityPublisher activityPublisher;

    public ChatAssistantServiceImpl(
            ChatService chatService,
            @Qualifier("primaryChatClient") ChatClient chatClient,
            ChatWorkflowEngine workflowEngine,
            ChatStreamActivityPublisher activityPublisher) {
        this.chatService = chatService;
        this.chatClient = chatClient;
        this.workflowEngine = workflowEngine;
        this.activityPublisher = activityPublisher;
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

        String sessionId = sessionId(userId, chatId);
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        activityPublisher.register(sessionId, emitter);
        emitter.onCompletion(() -> activityPublisher.unregister(sessionId));
        emitter.onTimeout(() -> activityPublisher.unregister(sessionId));
        emitter.onError(error -> activityPublisher.unregister(sessionId));

        sendSseEvent(emitter, "agent", Map.of("type", "agent_start", "agentId", AGENT_ID));

        HarnessResult beginResult = workflowEngine.beginTurn(sessionId, userMessage, emitter);
        if (!beginResult.success()) {
            sendErrorAndComplete(emitter, beginResult.message());
            return emitter;
        }

        String runId = beginResult.runId();
        StringBuilder fullResponse = new StringBuilder();

        Flux<ChatResponse> flux;
        try {
            flux = chatClient.prompt()
                    .user(userMessage)
                    .advisors(sessionAdvisors(userId, chatId))
                    .stream()
                    .chatResponse();
        } catch (RuntimeException ex) {
            activityPublisher.unregister(sessionId);
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
                error -> {
                    activityPublisher.unregister(sessionId);
                    sendErrorAndComplete(emitter, error);
                },
                () -> completeStream(emitter, sessionId, runId, chatId, fullResponse.toString()));

        return emitter;
    }

    private void completeStream(
            SseEmitter emitter, String sessionId, String runId, String chatId, String content) {
        HarnessResult harnessResult = workflowEngine.completeTurn(runId, sessionId, content, emitter);
        if (!harnessResult.success()) {
            activityPublisher.unregister(sessionId);
            sendErrorAndComplete(emitter, harnessResult.message());
            return;
        }

        ChatMessage assistant = chatService.appendAssistantMessage(
                chatId, content, estimateTokens(content), Map.of());
        sendSseEvent(emitter, "agent", Map.of("type", "agent_done", "agentId", AGENT_ID));
        sendSseEvent(emitter, "done", Map.of("id", assistant.id(), "content", content));
        activityPublisher.unregister(sessionId);
        emitter.complete();
    }

    private void sendErrorAndComplete(SseEmitter emitter, Throwable error) {
        sendErrorAndComplete(emitter, "LLM unavailable");
    }

    private void sendErrorAndComplete(SseEmitter emitter, String message) {
        sendSseEvent(emitter, "error", Map.of("message", message == null ? "LLM unavailable" : message));
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

    private static String sessionId(String userId, String chatId) {
        return userId + "-" + chatId;
    }

    private static java.util.function.Consumer<ChatClient.AdvisorSpec> sessionAdvisors(
            String userId, String chatId, java.util.List<String> enabledMcpConnections) {
        String sessionId = sessionId(userId, chatId);
        return spec -> spec
                .param(SessionMemoryAdvisor.SESSION_ID_CONTEXT_KEY, sessionId)
                .param(SessionMemoryAdvisor.USER_ID_CONTEXT_KEY, userId)
                .param(MCPToolAdvisor.ENABLED_CONNECTIONS_CONTEXT_KEY, enabledMcpConnections);
    }

    private java.util.function.Consumer<ChatClient.AdvisorSpec> sessionAdvisors(String userId, String chatId) {
        return sessionAdvisors(
                userId,
                chatId,
                chatService.getEnabledMcpConnections(userId, chatId));
    }
}
