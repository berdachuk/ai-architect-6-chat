package com.berdachuk.aichat.llm.harness.service;

import com.berdachuk.aichat.llm.harness.domain.PolicyDecision;
import org.springframework.stereotype.Service;

@Service
public class PolicyGateServiceImpl implements PolicyGateService {

    @Override
    public PolicyDecision review(String responseContent) {
        if (responseContent == null || responseContent.isBlank()) {
            return PolicyDecision.REJECT;
        }
        return PolicyDecision.ALLOW;
    }
}
