package com.berdachuk.aichat.llm.tool;

import com.berdachuk.aichat.llm.service.ChatStreamActivityPublisher;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.util.Map;

public class ActivityReportingToolCallback implements ToolCallback {

    private final ToolCallback delegate;
    private final ChatStreamActivityPublisher activityPublisher;
    private final String sessionId;

    public ActivityReportingToolCallback(
            ToolCallback delegate, ChatStreamActivityPublisher activityPublisher, String sessionId) {
        this.delegate = delegate;
        this.activityPublisher = activityPublisher;
        this.sessionId = sessionId;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return delegate.getToolDefinition();
    }

    @Override
    public String call(String toolInput) {
        return call(toolInput, new ToolContext(Map.of()));
    }

    @Override
    public String call(String toolInput, ToolContext toolContext) {
        String toolName = delegate.getToolDefinition().name();
        activityPublisher.publish(sessionId, "tool_call", Map.of(
                "toolName", toolName,
                "source", "mcp",
                "status", "running"));
        try {
            String result = delegate.call(toolInput, toolContext);
            activityPublisher.publish(sessionId, "tool_call", Map.of(
                    "toolName", toolName,
                    "source", "mcp",
                    "status", "done"));
            return result;
        } catch (RuntimeException ex) {
            activityPublisher.publish(sessionId, "tool_call", Map.of(
                    "toolName", toolName,
                    "source", "mcp",
                    "status", "failed",
                    "message", ex.getMessage() == null ? "Tool call failed" : ex.getMessage()));
            throw ex;
        }
    }
}
