package com.berdachuk.aichat.llm.harness.service;

import com.berdachuk.aichat.llm.harness.domain.PolicyDecision;

public interface PolicyGateService {

    PolicyDecision review(String responseContent);
}
