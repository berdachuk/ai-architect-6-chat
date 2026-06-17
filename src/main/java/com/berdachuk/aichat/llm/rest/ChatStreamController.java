package com.berdachuk.aichat.llm.rest;

import com.berdachuk.aichat.core.security.UserContext;
import com.berdachuk.aichat.llm.rest.dto.SendMessageRequest;
import com.berdachuk.aichat.llm.service.ChatAssistantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Tag(name = "Chat messages", description = "Streaming assistant responses")
@RestController
@RequestMapping("/api/v1/chats")
public class ChatStreamController {

    private final ChatAssistantService assistantService;
    private final UserContext userContext;

    public ChatStreamController(ChatAssistantService assistantService, UserContext userContext) {
        this.assistantService = assistantService;
        this.userContext = userContext;
    }

    @Operation(summary = "Stream assistant reply for a user message (SSE token/done events)")
    @PostMapping(value = "/{chatId}/messages/stream", produces = "text/event-stream")
    SseEmitter streamMessage(@PathVariable String chatId, @RequestBody SendMessageRequest request) {
        return assistantService.streamMessage(userContext.getUserId(), chatId, request.content());
    }
}
