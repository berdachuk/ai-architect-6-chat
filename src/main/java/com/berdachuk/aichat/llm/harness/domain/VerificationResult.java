package com.berdachuk.aichat.llm.harness.domain;

public record VerificationResult(boolean passed, String message) {
    public static VerificationResult ok() {
        return new VerificationResult(true, null);
    }
}
