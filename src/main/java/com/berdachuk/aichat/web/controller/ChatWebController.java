package com.berdachuk.aichat.web.controller;

import com.berdachuk.aichat.chat.domain.Chat;
import com.berdachuk.aichat.chat.service.ChatService;
import com.berdachuk.aichat.core.security.UserContext;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Controller
public class ChatWebController {

    private final ChatService chatService;
    private final UserContext userContext;

    public ChatWebController(ChatService chatService, UserContext userContext) {
        this.chatService = chatService;
        this.userContext = userContext;
    }

    @GetMapping("/")
    String home() {
        String userId = userContext.getUserId();
        Chat defaultChat = chatService.getOrCreateDefaultChat(userId);
        return "redirect:/chat/" + defaultChat.id();
    }

    @GetMapping("/chat/{chatId}")
    String chatView(@PathVariable String chatId, Model model) {
        String userId = userContext.getUserId();
        Chat chat = chatService.requireOwnedChat(userId, chatId);
        List<Chat> chats = chatService.listChats(userId);

        model.addAttribute("chat", chat);
        model.addAttribute("chats", chats);
        model.addAttribute("userId", userId);
        model.addAttribute("oauth2LoginEnabled", userContext.isOAuth2LoginEnabled());
        return "chat";
    }
}
