package com.berdachuk.aichat.llm.config;

import com.berdachuk.aichat.core.advisor.DateTimeContextAdvisor;
import org.springframework.ai.session.SessionService;
import org.springframework.ai.session.advisor.SessionMemoryAdvisor;
import org.springframework.ai.session.compaction.CompactionStrategy;
import org.springframework.ai.session.compaction.CompactionTrigger;
import org.springframework.ai.session.compaction.CompositeCompactionTrigger;
import org.springframework.ai.session.compaction.TokenCountTrigger;
import org.springframework.ai.session.compaction.TurnCountTrigger;
import org.springframework.ai.session.compaction.TurnWindowCompactionStrategy;
import org.springframework.ai.tokenizer.JTokkitTokenCountEstimator;
import org.springframework.ai.tokenizer.TokenCountEstimator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AgentSessionProperties.class)
public class SessionMemoryConfiguration {

    @Bean
    @ConditionalOnMissingBean(TokenCountEstimator.class)
    TokenCountEstimator sessionTokenCountEstimator() {
        return new JTokkitTokenCountEstimator();
    }

    @Bean
    CompactionTrigger sessionCompactionTrigger(
            AgentSessionProperties properties,
            TokenCountEstimator tokenCountEstimator) {
        AgentSessionProperties.Compaction compaction = properties.compaction();
        return CompositeCompactionTrigger.anyOf(
                new TurnCountTrigger(compaction.maxTurns()),
                TokenCountTrigger.builder()
                        .threshold(compaction.maxTokens())
                        .tokenCountEstimator(tokenCountEstimator)
                        .build());
    }

    @Bean
    CompactionStrategy sessionCompactionStrategy(
            AgentSessionProperties properties,
            TokenCountEstimator tokenCountEstimator) {
        return TurnWindowCompactionStrategy.builder()
                .maxTurns(properties.compaction().windowTurns())
                .tokenCountEstimator(tokenCountEstimator)
                .build();
    }

    @Bean
    SessionMemoryAdvisor sessionMemoryAdvisor(
            SessionService sessionService,
            CompactionTrigger sessionCompactionTrigger,
            CompactionStrategy sessionCompactionStrategy) {
        return SessionMemoryAdvisor.builder(sessionService)
                .compactionTrigger(sessionCompactionTrigger)
                .compactionStrategy(sessionCompactionStrategy)
                .build();
    }

    @Bean
    DateTimeContextAdvisor dateTimeContextAdvisor() {
        return new DateTimeContextAdvisor();
    }
}
