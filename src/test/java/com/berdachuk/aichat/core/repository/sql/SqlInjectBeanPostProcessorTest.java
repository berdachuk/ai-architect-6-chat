package com.berdachuk.aichat.core.repository.sql;

import com.berdachuk.aichat.chat.repository.impl.ChatRepositoryImpl;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SqlInjectBeanPostProcessorTest {

    @Test
    void injectsSqlFromClasspathIntoAnnotatedFields() throws Exception {
        var processor = new SqlInjectBeanPostProcessor();
        var repository = new ChatRepositoryImpl(mock(NamedParameterJdbcTemplate.class), mock(JsonMapper.class));

        processor.postProcessBeforeInitialization(repository, "chatRepositoryImpl");

        assertThat(readSqlField(repository, "selectByIdSql"))
                .contains("FROM ai_chat.chat")
                .contains(":id");
        assertThat(readSqlField(repository, "insertSql"))
                .contains("INSERT INTO ai_chat.chat");
    }

    private static String readSqlField(Object bean, String fieldName) throws Exception {
        Field field = bean.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return (String) field.get(bean);
    }
}
