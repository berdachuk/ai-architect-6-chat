package com.berdachuk.aichat.llm.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "spring.ai.session.jdbc")
public record AgentSessionProperties(
        String schema,
        Compaction compaction
) {
    public record Compaction(
            String strategy,
            Integer maxTurns,
            Integer maxTokens,
            Integer windowTurns
    ) {
        public Compaction {
            if (strategy == null) {
                strategy = "turn-window";
            }
            if (maxTurns == null) {
                maxTurns = 20;
            }
            if (maxTokens == null) {
                maxTokens = 4000;
            }
            if (windowTurns == null) {
                windowTurns = 30;
            }
        }
    }
}
