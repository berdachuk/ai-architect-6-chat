package com.berdachuk.aichat.mcp.service;

import com.berdachuk.aichat.mcp.config.McpBootstrapProperties;
import com.berdachuk.aichat.mcp.repository.McpConnectionRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class McpBootstrapSeederTest {

    @Test
    void seedsDefaultConnectionWhenCatalogEmpty() {
        McpConnectionRepository repository = mock(McpConnectionRepository.class);
        when(repository.findAll()).thenReturn(java.util.List.of());

        McpBootstrapProperties properties = new McpBootstrapProperties(
                true,
                "medical-dataset",
                "http://localhost:8092/sse",
                true,
                true,
                true);
        McpBootstrapSeeder seeder = new McpBootstrapSeeder(repository, properties);

        seeder.seedIfEmpty();

        org.mockito.Mockito.verify(repository).insert(org.mockito.ArgumentMatchers.argThat(
                connection -> "medical-dataset".equals(connection.name())
                        && connection.url().contains("8092")));
    }

    @Test
    void skipsSeedingWhenCatalogAlreadyPopulated() {
        McpConnectionRepository repository = mock(McpConnectionRepository.class);
        when(repository.findAll()).thenReturn(java.util.List.of(mock(com.berdachuk.aichat.mcp.domain.McpConnection.class)));

        McpBootstrapSeeder seeder = new McpBootstrapSeeder(
                repository,
                new McpBootstrapProperties(true, "medical-dataset", "http://localhost:8092/sse", true, true, true));

        seeder.seedIfEmpty();

        org.mockito.Mockito.verify(repository, org.mockito.Mockito.never()).insert(org.mockito.ArgumentMatchers.any());
    }
}
