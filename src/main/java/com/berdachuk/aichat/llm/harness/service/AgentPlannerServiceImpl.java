package com.berdachuk.aichat.llm.harness.service;

import com.berdachuk.aichat.core.util.IdGenerator;
import com.berdachuk.aichat.llm.harness.domain.AgentPlan;
import com.berdachuk.aichat.llm.harness.domain.PlanStep;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AgentPlannerServiceImpl implements AgentPlannerService {

    @Override
    public AgentPlan buildPlan(String sessionId, String userMessage) {
        return new AgentPlan(
                IdGenerator.generateId(),
                List.of(new PlanStep(1, "Understand the user request", null),
                        new PlanStep(2, "Compose a helpful response", null)),
                List.of("Response addresses the user message"));
    }
}
