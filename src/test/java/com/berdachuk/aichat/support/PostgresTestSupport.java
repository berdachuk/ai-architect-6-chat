package com.berdachuk.aichat.support;

import org.testcontainers.containers.PostgreSQLContainer;

public final class PostgresTestSupport {

    private PostgresTestSupport() {
    }

    public static String jdbcUrlWithSchema(PostgreSQLContainer<?> postgres) {
        String url = postgres.getJdbcUrl();
        String separator = url.contains("?") ? "&" : "?";
        return url + separator + "currentSchema=ai_chat";
    }
}
