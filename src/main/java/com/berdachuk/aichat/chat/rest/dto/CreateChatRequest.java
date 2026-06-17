package com.berdachuk.aichat.chat.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request to create a new chat session")
public record CreateChatRequest(
        @Schema(description = "Display name", example = "My Chat") String name,
        @Schema(description = "Agent identifier", example = "auto") String agentId) {
}
