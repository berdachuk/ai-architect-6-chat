package com.berdachuk.aichat.llm.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

public interface ChatStreamActivityPublisher {

    void register(String sessionId, SseEmitter emitter);

    void unregister(String sessionId);

    void publish(String sessionId, String type, Map<String, Object> fields);

    void publishReasoning(String sessionId, String message);
}
