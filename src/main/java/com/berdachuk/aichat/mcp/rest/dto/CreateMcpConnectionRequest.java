package com.berdachuk.aichat.mcp.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateMcpConnectionRequest(
        @NotBlank String name,
        @NotBlank
        @Pattern(regexp = "https?://(localhost|127\\.0\\.0\\.1|\\[::1\\]|[a-zA-Z0-9][-a-zA-Z0-9.]*\\.[a-zA-Z]{2,})(:\\d{1,5})?(/[-a-zA-Z0-9._~:/?#\\[\\]@!$&'()*+,;=%]*)?",
                message = "URL must use http/https scheme with a valid public or localhost hostname")
        String url,
        boolean tools,
        boolean resources,
        boolean prompts) {
}
