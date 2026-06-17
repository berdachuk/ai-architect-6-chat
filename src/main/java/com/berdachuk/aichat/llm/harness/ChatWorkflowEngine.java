package com.berdachuk.aichat.llm.harness;

import com.berdachuk.aichat.llm.harness.domain.HarnessResult;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface ChatWorkflowEngine {

    HarnessResult beginTurn(String sessionId, String userMessage, SseEmitter emitter);

    HarnessResult completeTurn(String runId, String sessionId, String responseContent, SseEmitter emitter);
}
