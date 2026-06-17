package com.berdachuk.aichat.chat.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request to rename a chat session")
public record RenameChatRequest(
        @Schema(description = "New display name", example = "Renamed Chat") String name) {
}
