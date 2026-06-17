package com.berdachuk.aichat.chat.rest.dto;

import java.util.List;

public record UpdateChatMcpRequest(List<String> connectionIds) {
}
