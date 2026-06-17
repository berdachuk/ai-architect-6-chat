package com.berdachuk.aichat.mcp.repository;

import com.berdachuk.aichat.mcp.domain.McpConnection;

import java.util.List;
import java.util.Optional;

public interface McpConnectionRepository {

    Optional<McpConnection> findById(String id);

    List<McpConnection> findAll();

    McpConnection insert(McpConnection connection);

    void deleteById(String id);

    boolean existsByName(String name);
}
