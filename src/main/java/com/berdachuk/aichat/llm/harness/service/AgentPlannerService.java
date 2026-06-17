package com.berdachuk.aichat.llm.harness.service;

import com.berdachuk.aichat.llm.harness.domain.AgentPlan;

public interface AgentPlannerService {

    AgentPlan buildPlan(String sessionId, String userMessage);
}
