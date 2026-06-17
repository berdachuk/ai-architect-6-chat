package com.berdachuk.aichat.llm.harness.service;

import com.berdachuk.aichat.llm.harness.domain.AgentPlan;
import com.berdachuk.aichat.llm.harness.domain.VerificationResult;
import org.springframework.stereotype.Service;

@Service
public class AgentResponseVerifierImpl implements AgentResponseVerifier {

    @Override
    public VerificationResult verify(AgentPlan plan, String responseContent) {
        return VerificationResult.ok();
    }
}
