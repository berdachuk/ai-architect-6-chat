package com.berdachuk.aichat.mcp.repository.impl;

import com.berdachuk.aichat.core.repository.sql.InjectSql;
import com.berdachuk.aichat.mcp.domain.McpConnection;
import com.berdachuk.aichat.mcp.repository.McpConnectionRepository;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class McpConnectionRepositoryImpl implements McpConnectionRepository {

    private final NamedParameterJdbcTemplate jdbc;

    @InjectSql("/sql/mcp/insert.sql")
    private String insertSql;

    @InjectSql("/sql/mcp/selectById.sql")
    private String selectByIdSql;

    @InjectSql("/sql/mcp/listAll.sql")
    private String listAllSql;

    @InjectSql("/sql/mcp/deleteById.sql")
    private String deleteByIdSql;

    @InjectSql("/sql/mcp/existsByName.sql")
    private String existsByNameSql;

    public McpConnectionRepositoryImpl(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<McpConnection> findById(String id) {
        List<McpConnection> results = jdbc.query(selectByIdSql, Map.of("id", id), new McpConnectionRowMapper());
        return results.isEmpty() ? Optional.empty() : Optional.of(results.getFirst());
    }

    @Override
    public List<McpConnection> findAll() {
        return jdbc.query(listAllSql, Map.of(), new McpConnectionRowMapper());
    }

    @Override
    public McpConnection insert(McpConnection connection) {
        jdbc.update(insertSql, Map.of(
                "id", connection.id(),
                "name", connection.name(),
                "url", connection.url(),
                "toolsEnabled", connection.toolsEnabled(),
                "resourcesEnabled", connection.resourcesEnabled(),
                "promptsEnabled", connection.promptsEnabled(),
                "createdAt", Timestamp.from(connection.createdAt()),
                "updatedAt", Timestamp.from(connection.updatedAt())));
        return connection;
    }

    @Override
    public void deleteById(String id) {
        jdbc.update(deleteByIdSql, Map.of("id", id));
    }

    @Override
    public boolean existsByName(String name) {
        Boolean exists = jdbc.queryForObject(existsByNameSql, Map.of("name", name), Boolean.class);
        return Boolean.TRUE.equals(exists);
    }

    private static class McpConnectionRowMapper implements RowMapper<McpConnection> {

        @Override
        public McpConnection mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new McpConnection(
                    rs.getString("id"),
                    rs.getString("name"),
                    rs.getString("url"),
                    rs.getBoolean("tools_enabled"),
                    rs.getBoolean("resources_enabled"),
                    rs.getBoolean("prompts_enabled"),
                    rs.getTimestamp("created_at").toInstant(),
                    rs.getTimestamp("updated_at").toInstant());
        }
    }
}
