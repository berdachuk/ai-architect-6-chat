package com.berdachuk.aichat.llm.harness.domain;

public record PlanStep(
        int order,
        String description,
        String toolName
) {
}
