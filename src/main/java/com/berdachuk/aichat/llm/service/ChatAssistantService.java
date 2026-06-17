package com.berdachuk.aichat.llm.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface ChatAssistantService {

    String processMessage(String userId, String chatId, String userMessage);

    SseEmitter streamMessage(String userId, String chatId, String userMessage);
}
