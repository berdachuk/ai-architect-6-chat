package com.berdachuk.aichat.llm.harness.service;

import com.berdachuk.aichat.llm.harness.domain.AgentPlan;
import com.berdachuk.aichat.llm.harness.domain.VerificationResult;

public interface AgentResponseVerifier {

    VerificationResult verify(AgentPlan plan, String responseContent);
}
