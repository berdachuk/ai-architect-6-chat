package com.berdachuk.aichat.chat.repository.impl;

import com.berdachuk.aichat.chat.domain.Chat;
import com.berdachuk.aichat.chat.repository.ChatRepository;
import com.berdachuk.aichat.core.repository.sql.InjectSql;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class ChatRepositoryImpl implements ChatRepository {

    private static final TypeReference<List<String>> CONNECTION_IDS_TYPE = new TypeReference<>() {};

    private final NamedParameterJdbcTemplate jdbc;
    private final JsonMapper jsonMapper;

    @InjectSql("/sql/chat/insert.sql")
    private String insertSql;

    @InjectSql("/sql/chat/selectById.sql")
    private String selectByIdSql;

    @InjectSql("/sql/chat/listByUser.sql")
    private String listByUserSql;

    @InjectSql("/sql/chat/findDefaultByUser.sql")
    private String findDefaultByUserSql;

    @InjectSql("/sql/chat/deleteById.sql")
    private String deleteByIdSql;

    @InjectSql("/sql/chat/updateName.sql")
    private String updateNameSql;

    @InjectSql("/sql/chat/updateAgentId.sql")
    private String updateAgentIdSql;

    @InjectSql("/sql/chat/updateActivity.sql")
    private String updateActivitySql;

    @InjectSql("/sql/chat/updateEnabledMcpConnections.sql")
    private String updateEnabledMcpConnectionsSql;

    @InjectSql("/sql/chat/existsByUserId.sql")
    private String existsByUserIdSql;

    public ChatRepositoryImpl(NamedParameterJdbcTemplate jdbc, JsonMapper jsonMapper) {
        this.jdbc = jdbc;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public Optional<Chat> findById(String id) {
        List<Chat> results = jdbc.query(selectByIdSql, Map.of("id", id), new ChatRowMapper(jsonMapper));
        return results.isEmpty() ? Optional.empty() : Optional.of(results.getFirst());
    }

    @Override
    public List<Chat> findByUserId(String userId) {
        return jdbc.query(listByUserSql, Map.of("userId", userId), new ChatRowMapper(jsonMapper));
    }

    @Override
    public Optional<Chat> findDefaultByUserId(String userId) {
        List<Chat> results = jdbc.query(findDefaultByUserSql, Map.of("userId", userId), new ChatRowMapper(jsonMapper));
        return results.isEmpty() ? Optional.empty() : Optional.of(results.getFirst());
    }

    @Override
    public Chat insert(Chat chat) {
        jdbc.update(insertSql, Map.of(
                "id", chat.id(),
                "userId", chat.userId(),
                "name", chat.name(),
                "agentId", chat.agentId(),
                "isDefault", chat.isDefault(),
                "createdAt", Timestamp.from(chat.createdAt()),
                "updatedAt", Timestamp.from(chat.updatedAt()),
                "lastActivityAt", Timestamp.from(chat.lastActivityAt()),
                "messageCount", chat.messageCount(),
                "enabledMcpConnections", toJson(chat.enabledMcpConnections())));
        return chat;
    }

    @Override
    public void updateName(String id, String name) {
        jdbc.update(updateNameSql, Map.of("id", id, "name", name));
    }

    @Override
    public void updateAgentId(String id, String agentId) {
        jdbc.update(updateAgentIdSql, Map.of("id", id, "agentId", agentId));
    }

    @Override
    public void updateActivity(String id, Instant lastActivityAt, int messageCount) {
        jdbc.update(updateActivitySql, Map.of(
                "id", id,
                "lastActivityAt", Timestamp.from(lastActivityAt),
                "messageCount", messageCount));
    }

    @Override
    public void updateEnabledMcpConnections(String id, List<String> connectionIds) {
        jdbc.update(updateEnabledMcpConnectionsSql, Map.of(
                "id", id,
                "enabledMcpConnections", toJson(connectionIds)));
    }

    @Override
    public void deleteById(String id) {
        jdbc.update(deleteByIdSql, Map.of("id", id));
    }

    @Override
    public boolean existsByUserId(String userId) {
        Boolean exists = jdbc.queryForObject(existsByUserIdSql, Map.of("userId", userId), Boolean.class);
        return Boolean.TRUE.equals(exists);
    }

    private String toJson(List<String> connectionIds) {
        try {
            return jsonMapper.writeValueAsString(connectionIds == null ? List.of() : connectionIds);
        } catch (JacksonException e) {
            throw new IllegalArgumentException("Invalid MCP connection ids", e);
        }
    }

    private static class ChatRowMapper implements RowMapper<Chat> {

        private final JsonMapper jsonMapper;

        private ChatRowMapper(JsonMapper jsonMapper) {
            this.jsonMapper = jsonMapper;
        }

        @Override
        public Chat mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new Chat(
                    rs.getString("id"),
                    rs.getString("user_id"),
                    rs.getString("name"),
                    rs.getString("agent_id"),
                    rs.getBoolean("is_default"),
                    rs.getTimestamp("created_at").toInstant(),
                    rs.getTimestamp("updated_at").toInstant(),
                    rs.getTimestamp("last_activity_at").toInstant(),
                    rs.getInt("message_count"),
                    parseConnectionIds(rs.getString("enabled_mcp_connections"), jsonMapper));
        }

        private static List<String> parseConnectionIds(String json, JsonMapper jsonMapper) {
            if (json == null || json.isBlank()) {
                return List.of();
            }
            try {
                return jsonMapper.readValue(json, CONNECTION_IDS_TYPE);
            } catch (JacksonException e) {
                throw new IllegalStateException("Failed to parse enabled MCP connections", e);
            }
        }
    }
}
