package com.berdachuk.aichat.llm.harness.domain;

public record HarnessResult(
        boolean success,
        String runId,
        String message
) {
    public static HarnessResult success(String runId) {
        return new HarnessResult(true, runId, null);
    }

    public static HarnessResult failed(String runId, String message) {
        return new HarnessResult(false, runId, message);
    }
}
