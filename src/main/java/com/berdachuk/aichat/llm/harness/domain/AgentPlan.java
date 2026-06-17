package com.berdachuk.aichat.llm.harness.domain;

import java.util.List;

public record AgentPlan(
        String planId,
        List<PlanStep> steps,
        List<String> acceptanceCriteria
) {
}
