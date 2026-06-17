package com.berdachuk.aichat.mcp.rest;

import com.berdachuk.aichat.mcp.rest.dto.CreateMcpConnectionRequest;
import com.berdachuk.aichat.mcp.rest.dto.McpConnectionView;
import com.berdachuk.aichat.mcp.service.McpConnectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "MCP", description = "Runtime MCP connection catalog")
@RestController
@RequestMapping("/api/v1/mcp/connections")
@ConditionalOnProperty(name = "ai-chat.features.mcp-client", havingValue = "true", matchIfMissing = true)
public class McpConnectionController {

    private final McpConnectionService connectionService;

    public McpConnectionController(McpConnectionService connectionService) {
        this.connectionService = connectionService;
    }

    @Operation(summary = "List MCP connections in the runtime catalog")
    @GetMapping
    List<McpConnectionView> listConnections() {
        return connectionService.listConnections();
    }

    @Operation(summary = "Register a new MCP connection at runtime")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    McpConnectionView createConnection(@Valid @RequestBody CreateMcpConnectionRequest request) {
        return connectionService.createConnection(request);
    }

    @Operation(summary = "Remove an MCP connection from the catalog")
    @DeleteMapping("/{connectionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteConnection(@PathVariable String connectionId) {
        connectionService.deleteConnection(connectionId);
    }
}
