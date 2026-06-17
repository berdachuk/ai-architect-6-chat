package com.berdachuk.aichat.llm.harness.service;

import com.berdachuk.aichat.llm.harness.domain.PolicyDecision;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PolicyGateServiceImplTest {

    private final PolicyGateServiceImpl policyGate = new PolicyGateServiceImpl();

    @Test
    void rejectsBlankResponse() {
        assertThat(policyGate.review("")).isEqualTo(PolicyDecision.REJECT);
        assertThat(policyGate.review("   ")).isEqualTo(PolicyDecision.REJECT);
    }

    @Test
    void allowsNonBlankResponse() {
        assertThat(policyGate.review("Hello")).isEqualTo(PolicyDecision.ALLOW);
    }
}
