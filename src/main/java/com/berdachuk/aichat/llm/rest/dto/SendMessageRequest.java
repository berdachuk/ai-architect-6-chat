package com.berdachuk.aichat.llm.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "User message to send to the assistant")
public record SendMessageRequest(
        @Schema(description = "Message text", example = "Hello") String content) {
}
