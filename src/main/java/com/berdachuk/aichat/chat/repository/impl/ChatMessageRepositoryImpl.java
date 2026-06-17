package com.berdachuk.aichat.chat.repository.impl;

import com.berdachuk.aichat.chat.domain.ChatMessage;
import com.berdachuk.aichat.chat.repository.ChatMessageRepository;
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
import java.util.List;
import java.util.Map;

@Repository
public class ChatMessageRepositoryImpl implements ChatMessageRepository {

    private static final TypeReference<Map<String, Object>> METADATA_TYPE = new TypeReference<>() {};

    private final NamedParameterJdbcTemplate jdbc;
    private final JsonMapper jsonMapper;

    @InjectSql("/sql/chat/insertMessage.sql")
    private String insertMessageSql;

    @InjectSql("/sql/chat/selectMessages.sql")
    private String selectMessagesSql;

    @InjectSql("/sql/chat/getNextSequenceNumber.sql")
    private String getNextSequenceNumberSql;

    @InjectSql("/sql/chat/softDeleteMessages.sql")
    private String softDeleteMessagesSql;

    @InjectSql("/sql/chat/countMessages.sql")
    private String countMessagesSql;

    public ChatMessageRepositoryImpl(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
        this.jsonMapper = JsonMapper.builder().build();
    }

    @Override
    public List<ChatMessage> findByChatId(String chatId, int limit, int offset) {
        return jdbc.query(selectMessagesSql, Map.of(
                "chatId", chatId,
                "limit", limit,
                "offset", offset), new ChatMessageRowMapper(jsonMapper));
    }

    @Override
    public ChatMessage insert(ChatMessage message) {
        jdbc.update(insertMessageSql, Map.of(
                "id", message.id(),
                "chatId", message.chatId(),
                "role", message.role(),
                "content", message.content(),
                "sequenceNumber", message.sequenceNumber(),
                "tokensUsed", message.tokensUsed(),
                "createdAt", Timestamp.from(message.createdAt()),
                "metadata", toJson(message.metadata())));
        return message;
    }

    @Override
    public int getNextSequenceNumber(String chatId) {
        Integer next = jdbc.queryForObject(getNextSequenceNumberSql, Map.of("chatId", chatId), Integer.class);
        return next != null ? next : 1;
    }

    @Override
    public void softDeleteByChatId(String chatId) {
        jdbc.update(softDeleteMessagesSql, Map.of("chatId", chatId));
    }

    @Override
    public int countByChatId(String chatId) {
        Integer count = jdbc.queryForObject(countMessagesSql, Map.of("chatId", chatId), Integer.class);
        return count != null ? count : 0;
    }

    private String toJson(Map<String, Object> metadata) {
        try {
            return jsonMapper.writeValueAsString(metadata != null ? metadata : Map.of());
        } catch (JacksonException e) {
            throw new IllegalArgumentException("Invalid message metadata", e);
        }
    }

    private static class ChatMessageRowMapper implements RowMapper<ChatMessage> {

        private final JsonMapper jsonMapper;

        private ChatMessageRowMapper(JsonMapper jsonMapper) {
            this.jsonMapper = jsonMapper;
        }

        @Override
        public ChatMessage mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new ChatMessage(
                    rs.getString("id"),
                    rs.getString("chat_id"),
                    rs.getString("role"),
                    rs.getString("content"),
                    rs.getInt("sequence_number"),
                    (Integer) rs.getObject("tokens_used"),
                    rs.getTimestamp("created_at").toInstant(),
                    parseMetadata(rs.getString("metadata"), jsonMapper));
        }

        private static Map<String, Object> parseMetadata(String json, JsonMapper jsonMapper) {
            if (json == null || json.isBlank()) {
                return Map.of();
            }
            try {
                return jsonMapper.readValue(json, METADATA_TYPE);
            } catch (JacksonException e) {
                throw new IllegalStateException("Failed to parse message metadata", e);
            }
        }
    }
}
