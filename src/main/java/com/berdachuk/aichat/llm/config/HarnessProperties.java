package com.berdachuk.aichat.llm.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai-chat.harness")
public record HarnessProperties(
        Integer maxIterations,
        Boolean retryOnVerifyFail,
        Boolean policyGateEnabled,
        Boolean humanCheckpointEnabled
) {
    public HarnessProperties {
        if (maxIterations == null) {
            maxIterations = 2;
        }
        if (retryOnVerifyFail == null) {
            retryOnVerifyFail = true;
        }
        if (policyGateEnabled == null) {
            policyGateEnabled = true;
        }
        if (humanCheckpointEnabled == null) {
            humanCheckpointEnabled = false;
        }
    }
}
