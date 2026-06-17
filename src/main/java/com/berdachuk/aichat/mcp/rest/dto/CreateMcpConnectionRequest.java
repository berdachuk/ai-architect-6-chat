package com.berdachuk.aichat.mcp.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateMcpConnectionRequest(
        @NotBlank String name,
        @NotBlank String url,
        boolean tools,
        boolean resources,
        boolean prompts) {
}
