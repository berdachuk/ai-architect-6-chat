package com.berdachuk.aichat.llm.harness.store;

import java.util.Map;

public interface HarnessChainTraceStore {

    void record(String runId, String eventType, Map<String, Object> payload);
}
