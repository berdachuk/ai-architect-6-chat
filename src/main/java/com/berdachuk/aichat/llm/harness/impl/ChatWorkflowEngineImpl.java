package com.berdachuk.aichat.llm.harness.impl;

import com.berdachuk.aichat.core.util.IdGenerator;
import com.berdachuk.aichat.llm.config.HarnessProperties;
import com.berdachuk.aichat.llm.harness.ChatWorkflowEngine;
import com.berdachuk.aichat.llm.harness.domain.AgentPlan;
import com.berdachuk.aichat.llm.harness.domain.HarnessResult;
import com.berdachuk.aichat.llm.harness.domain.PolicyDecision;
import com.berdachuk.aichat.llm.harness.domain.VerificationResult;
import com.berdachuk.aichat.llm.harness.service.AgentPlannerService;
import com.berdachuk.aichat.llm.harness.service.AgentResponseVerifier;
import com.berdachuk.aichat.llm.harness.service.PolicyGateService;
import com.berdachuk.aichat.llm.harness.store.HarnessChainTraceStore;
import com.berdachuk.aichat.llm.harness.store.HarnessWorkflowRunStore;
import com.berdachuk.aichat.llm.service.ChatStreamActivityPublisher;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ChatWorkflowEngineImpl implements ChatWorkflowEngine {

    private static final String AGENT_ID = "chat-orchestrator";

    private final AgentPlannerService planner;
    private final AgentResponseVerifier verifier;
    private final PolicyGateService policyGate;
    private final HarnessWorkflowRunStore runStore;
    private final HarnessChainTraceStore traceStore;
    private final ChatStreamActivityPublisher activityPublisher;
    private final HarnessProperties properties;

    public ChatWorkflowEngineImpl(
            AgentPlannerService planner,
            AgentResponseVerifier verifier,
            PolicyGateService policyGate,
            HarnessWorkflowRunStore runStore,
            HarnessChainTraceStore traceStore,
            ChatStreamActivityPublisher activityPublisher,
            HarnessProperties properties) {
        this.planner = planner;
        this.verifier = verifier;
        this.policyGate = policyGate;
        this.runStore = runStore;
        this.traceStore = traceStore;
        this.activityPublisher = activityPublisher;
        this.properties = properties;
    }

    @Override
    public HarnessResult beginTurn(String sessionId, String userMessage, SseEmitter emitter) {
        String runId = IdGenerator.generateId();
        runStore.save(runId, sessionId, "TASK_CREATED", null);

        traceStore.record(runId, "TASK_CREATED", Map.of("message", userMessage));
        emitStage(emitter, "TASK_CREATED", "running");

        emitStage(emitter, "PLANNING", "running");
        AgentPlan plan = planner.buildPlan(sessionId, userMessage);
        runStore.updateStateAndPlan(runId, "PLANNING", plan);
        traceStore.record(runId, "PLANNING", Map.of("planId", plan.planId()));
        publishPlan(sessionId, plan);

        emitStage(emitter, "CONTEXT_BUILT", "done");
        traceStore.record(runId, "CONTEXT_BUILT", Map.of());

        emitStage(emitter, "TOOLS_EXECUTED", "pending");
        return HarnessResult.success(runId);
    }

    @Override
    public HarnessResult completeTurn(String runId, String sessionId, String responseContent, SseEmitter emitter) {
        emitStage(emitter, "TOOLS_EXECUTED", "done");

        emitStage(emitter, "VERIFYING", "running");
        VerificationResult verification = verifier.verify(null, responseContent);
        traceStore.record(runId, "VERIFYING", Map.of("passed", verification.passed()));
        if (!verification.passed()) {
            runStore.updateState(runId, "FAILED");
            emitStage(emitter, "FAILED", "failed");
            return HarnessResult.failed(runId, verification.message());
        }

        if (properties.policyGateEnabled()) {
            emitStage(emitter, "POLICY_GATE", "running");
            PolicyDecision decision = policyGate.review(responseContent);
            traceStore.record(runId, "POLICY_GATE", Map.of("decision", decision.name()));
            if (decision == PolicyDecision.REJECT) {
                runStore.updateState(runId, "FAILED");
                emitStage(emitter, "FAILED", "failed");
                activityPublisher.publish(sessionId, "policy_reject",
                        Map.of("message", "Policy gate rejected empty response"));
                return HarnessResult.failed(runId, "Policy gate rejected response");
            }
        }

        runStore.updateState(runId, "DONE");
        emitStage(emitter, "DONE", "done");
        traceStore.record(runId, "DONE", Map.of());
        return HarnessResult.success(runId);
    }

    private void publishPlan(String sessionId, AgentPlan plan) {
        List<Map<String, Object>> steps = plan.steps().stream()
                .map(step -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("order", step.order());
                    item.put("description", step.description());
                    if (step.toolName() != null) {
                        item.put("tool", step.toolName());
                    }
                    item.put("status", "pending");
                    return item;
                })
                .toList();
        activityPublisher.publish(sessionId, "todo_update", Map.of("steps", steps));
    }

    private void emitStage(SseEmitter emitter, String stage, String status) {
        sendEvent(emitter, "pipeline_stage", Map.of(
                "stage", stage,
                "agent", AGENT_ID,
                "status", status,
                "timestampMs", System.currentTimeMillis()));
    }

    private static void sendEvent(SseEmitter emitter, String event, Object data) {
        try {
            emitter.send(SseEmitter.event().name(event).data(data, MediaType.APPLICATION_JSON));
        } catch (IOException ex) {
            emitter.completeWithError(ex);
        }
    }
}
