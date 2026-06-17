package com.berdachuk.aichat.llm.harness.store;

import com.berdachuk.aichat.llm.harness.domain.AgentPlan;

public interface HarnessWorkflowRunStore {

    void save(String runId, String sessionId, String state, AgentPlan plan);

    void updateState(String runId, String state);

    void updateStateAndPlan(String runId, String state, AgentPlan plan);
}
