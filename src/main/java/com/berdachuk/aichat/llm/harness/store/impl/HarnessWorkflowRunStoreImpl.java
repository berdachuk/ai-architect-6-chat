package com.berdachuk.aichat.llm.harness.store.impl;

import com.berdachuk.aichat.core.repository.sql.InjectSql;
import com.berdachuk.aichat.llm.harness.domain.AgentPlan;
import com.berdachuk.aichat.llm.harness.store.HarnessWorkflowRunStore;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;

@Repository
public class HarnessWorkflowRunStoreImpl implements HarnessWorkflowRunStore {

    private final NamedParameterJdbcTemplate jdbc;
    private final JsonMapper jsonMapper;

    @InjectSql("/sql/harness/insertRun.sql")
    private String insertRunSql;

    @InjectSql("/sql/harness/updateRunState.sql")
    private String updateRunStateSql;

    @InjectSql("/sql/harness/updateRunWithPlan.sql")
    private String updateRunWithPlanSql;

    public HarnessWorkflowRunStoreImpl(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
        this.jsonMapper = JsonMapper.builder().build();
    }

    @Override
    public void save(String runId, String sessionId, String state, AgentPlan plan) {
        jdbc.update(insertRunSql, new MapSqlParameterSource()
                .addValue("runId", runId)
                .addValue("sessionId", sessionId)
                .addValue("state", state)
                .addValue("planJson", serialize(plan)));
    }

    @Override
    public void updateState(String runId, String state) {
        jdbc.update(updateRunStateSql, Map.of("runId", runId, "state", state));
    }

    @Override
    public void updateStateAndPlan(String runId, String state, AgentPlan plan) {
        jdbc.update(updateRunWithPlanSql, new MapSqlParameterSource()
                .addValue("runId", runId)
                .addValue("state", state)
                .addValue("planJson", serialize(plan)));
    }

    private String serialize(AgentPlan plan) {
        if (plan == null) {
            return null;
        }
        try {
            return jsonMapper.writeValueAsString(plan);
        } catch (RuntimeException ex) {
            throw new IllegalStateException("Failed to serialize harness plan", ex);
        }
    }
}
