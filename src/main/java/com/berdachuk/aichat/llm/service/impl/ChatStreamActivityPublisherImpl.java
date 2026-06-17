package com.berdachuk.aichat.llm.service.impl;

import com.berdachuk.aichat.llm.service.ChatStreamActivityPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChatStreamActivityPublisherImpl implements ChatStreamActivityPublisher {

    private static final Logger log = LoggerFactory.getLogger(ChatStreamActivityPublisherImpl.class);

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    @Override
    public void register(String sessionId, SseEmitter emitter) {
        if (sessionId == null || sessionId.isBlank() || emitter == null) {
            return;
        }
        emitters.put(sessionId, emitter);
    }

    @Override
    public void unregister(String sessionId) {
        if (sessionId != null) {
            emitters.remove(sessionId);
        }
    }

    @Override
    public void publish(String sessionId, String type, Map<String, Object> fields) {
        SseEmitter emitter = emitters.get(sessionId);
        if (emitter == null) {
            return;
        }
        Map<String, Object> payload = new LinkedHashMap<>(fields);
        payload.put("type", type);
        try {
            emitter.send(SseEmitter.event().name("activity").data(payload, MediaType.APPLICATION_JSON));
        } catch (IOException ex) {
            log.debug("Failed to send activity event for session {}: {}", sessionId, ex.getMessage());
            emitters.remove(sessionId);
        }
    }

    @Override
    public void publishReasoning(String sessionId, String message) {
        publish(sessionId, "reasoning", Map.of("message", message));
    }
}
