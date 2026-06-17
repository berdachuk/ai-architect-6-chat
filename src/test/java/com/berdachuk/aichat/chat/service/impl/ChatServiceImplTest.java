package com.berdachuk.aichat.chat.service.impl;

import com.berdachuk.aichat.chat.domain.Chat;
import com.berdachuk.aichat.chat.exception.ChatNotFoundException;
import com.berdachuk.aichat.chat.repository.ChatMessageRepository;
import com.berdachuk.aichat.chat.repository.ChatRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatServiceImplTest {

    @Mock
    private ChatRepository chatRepository;

    @Mock
    private ChatMessageRepository messageRepository;

    @InjectMocks
    private ChatServiceImpl chatService;

    @Test
    void createChat_marksFirstChatAsDefault() {
        when(chatRepository.existsByUserId("user-1")).thenReturn(false);
        when(chatRepository.insert(any(Chat.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Chat created = chatService.createChat("user-1", "Hello", "auto");

        assertThat(created.userId()).isEqualTo("user-1");
        assertThat(created.name()).isEqualTo("Hello");
        assertThat(created.isDefault()).isTrue();
    }

    @Test
    void deleteChat_recreatesDefaultWhenLastChatRemoved() {
        Chat chat = sampleChat("chat-1", "user-1", true);
        when(chatRepository.findById("chat-1")).thenReturn(Optional.of(chat));
        when(chatRepository.findByUserId("user-1")).thenReturn(List.of(), List.of());
        when(chatRepository.existsByUserId("user-1")).thenReturn(false);
        when(chatRepository.insert(any(Chat.class))).thenAnswer(invocation -> invocation.getArgument(0));

        chatService.deleteChat("user-1", "chat-1");

        verify(messageRepository).softDeleteByChatId("chat-1");
        verify(chatRepository).deleteById("chat-1");
        verify(chatRepository).insert(any(Chat.class));
    }

    @Test
    void requireOwnedChat_throwsWhenChatMissingOrWrongOwner() {
        when(chatRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.requireOwnedChat("user-1", "missing"))
                .isInstanceOf(ChatNotFoundException.class);

        Chat otherUserChat = sampleChat("chat-2", "user-2", false);
        when(chatRepository.findById("chat-2")).thenReturn(Optional.of(otherUserChat));

        assertThatThrownBy(() -> chatService.requireOwnedChat("user-1", "chat-2"))
                .isInstanceOf(ChatNotFoundException.class);
    }

    @Test
    void renameChat_updatesNameForOwnedChat() {
        Chat chat = sampleChat("chat-1", "user-1", true);
        when(chatRepository.findById("chat-1")).thenReturn(Optional.of(chat));

        chatService.renameChat("user-1", "chat-1", "Renamed");

        verify(chatRepository).updateName("chat-1", "Renamed");
    }

    @Test
    void getHistory_requiresOwnership() {
        Chat chat = sampleChat("chat-1", "user-1", true);
        when(chatRepository.findById("chat-1")).thenReturn(Optional.of(chat));
        when(messageRepository.findByChatId("chat-1", 10, 0)).thenReturn(List.of());

        chatService.getHistory("user-1", "chat-1", 10, 0);

        verify(messageRepository).findByChatId(eq("chat-1"), eq(10), eq(0));
        verify(chatRepository, never()).updateName(any(), any());
    }

    private static Chat sampleChat(String id, String userId, boolean isDefault) {
        Instant now = Instant.parse("2026-06-17T10:00:00Z");
        return new Chat(id, userId, "Chat", "auto", isDefault, now, now, now, 0, List.of());
    }
}
