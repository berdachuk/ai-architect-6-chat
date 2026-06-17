package com.berdachuk.aichat.llm.harness.store.impl;

import com.berdachuk.aichat.core.repository.sql.InjectSql;
import com.berdachuk.aichat.core.util.IdGenerator;
import com.berdachuk.aichat.llm.harness.domain.AgentPlan;
import com.berdachuk.aichat.llm.harness.store.HarnessChainTraceStore;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;

@Repository
public class HarnessChainTraceStoreImpl implements HarnessChainTraceStore {

    private final NamedParameterJdbcTemplate jdbc;
    private final JsonMapper jsonMapper;

    @InjectSql("/sql/harness/insertTrace.sql")
    private String insertTraceSql;

    public HarnessChainTraceStoreImpl(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
        this.jsonMapper = JsonMapper.builder().build();
    }

    @Override
    public void record(String runId, String eventType, Map<String, Object> payload) {
        jdbc.update(insertTraceSql, new MapSqlParameterSource()
                .addValue("id", IdGenerator.generateId())
                .addValue("runId", runId)
                .addValue("eventType", eventType)
                .addValue("payload", serialize(payload)));
    }

    private String serialize(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return null;
        }
        try {
            return jsonMapper.writeValueAsString(payload);
        } catch (RuntimeException ex) {
            throw new IllegalStateException("Failed to serialize harness trace payload", ex);
        }
    }
}
