package com.berdachuk.aichat.llm.tool;

import com.berdachuk.aichat.llm.service.ChatStreamActivityPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ActivityReportingToolCallbackTest {

    @Test
    void publishesRunningAndDoneActivityEvents() {
        ToolCallback delegate = mock(ToolCallback.class);
        ToolDefinition definition = mock(ToolDefinition.class);
        when(definition.name()).thenReturn("echo");
        when(delegate.getToolDefinition()).thenReturn(definition);
        when(delegate.call(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any()))
                .thenReturn("ok");

        ChatStreamActivityPublisher publisher = mock(ChatStreamActivityPublisher.class);
        ActivityReportingToolCallback callback =
                new ActivityReportingToolCallback(delegate, publisher, "user-chat");

        String result = callback.call("{\"input\":\"hi\"}");

        org.assertj.core.api.Assertions.assertThat(result).isEqualTo("ok");
        verify(publisher).publish(eq("user-chat"), eq("tool_call"), org.mockito.ArgumentMatchers.argThat(
                fields -> "running".equals(fields.get("status")) && "echo".equals(fields.get("toolName"))));
        verify(publisher).publish(eq("user-chat"), eq("tool_call"), org.mockito.ArgumentMatchers.argThat(
                fields -> "done".equals(fields.get("status"))));
    }

    @Test
    void publishesFailureActivityWhenDelegateThrows() {
        ToolCallback delegate = mock(ToolCallback.class);
        ToolDefinition definition = mock(ToolDefinition.class);
        when(definition.name()).thenReturn("echo");
        when(delegate.getToolDefinition()).thenReturn(definition);
        when(delegate.call(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any()))
                .thenThrow(new IllegalStateException("boom"));

        ChatStreamActivityPublisher publisher = mock(ChatStreamActivityPublisher.class);
        ActivityReportingToolCallback callback =
                new ActivityReportingToolCallback(delegate, publisher, "user-chat");

        assertThatThrownBy(() -> callback.call("{}"))
                .isInstanceOf(IllegalStateException.class);

        verify(publisher).publish(eq("user-chat"), eq("tool_call"), org.mockito.ArgumentMatchers.argThat(
                fields -> "failed".equals(fields.get("status")) && "boom".equals(fields.get("message"))));
    }
}
