package com.berdachuk.aichat.chat.rest;

import com.berdachuk.aichat.chat.domain.Chat;
import com.berdachuk.aichat.chat.domain.ChatMessage;
import com.berdachuk.aichat.chat.rest.dto.CreateChatRequest;
import com.berdachuk.aichat.chat.rest.dto.RenameChatRequest;
import com.berdachuk.aichat.chat.service.ChatService;
import com.berdachuk.aichat.core.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Chats", description = "Chat session CRUD and message history")
@RestController
@RequestMapping("/api/v1/chats")
public class ChatController {

    private final ChatService chatService;
    private final UserContext userContext;

    public ChatController(ChatService chatService, UserContext userContext) {
        this.chatService = chatService;
        this.userContext = userContext;
    }

    @Operation(summary = "List chats for the current user")
    @GetMapping
    List<Chat> listChats() {
        return chatService.listChats(userContext.getUserId());
    }

    @Operation(summary = "Create a new chat session")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    Chat createChat(@RequestBody(required = false) CreateChatRequest request) {
        CreateChatRequest req = request != null ? request : new CreateChatRequest(null, null);
        return chatService.createChat(userContext.getUserId(), req.name(), req.agentId());
    }

    @Operation(summary = "Get message history for a chat")
    @GetMapping("/{chatId}/history")
    List<ChatMessage> history(
            @PathVariable String chatId,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        return chatService.getHistory(userContext.getUserId(), chatId, limit, offset);
    }

    @Operation(summary = "Delete a chat session")
    @DeleteMapping("/{chatId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteChat(@PathVariable String chatId) {
        chatService.deleteChat(userContext.getUserId(), chatId);
    }

    @Operation(summary = "Rename a chat session")
    @PutMapping("/{chatId}/name")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void renameChat(@PathVariable String chatId, @RequestBody RenameChatRequest request) {
        chatService.renameChat(userContext.getUserId(), chatId, request.name());
    }
}
