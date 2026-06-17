package com.berdachuk.aichat.mcp.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@ConditionalOnProperty(name = "ai-chat.features.mcp-client", havingValue = "true", matchIfMissing = true)
public class McpConnectionConnectListener {

    private final McpClientConnector connector;

    public McpConnectionConnectListener(McpClientConnector connector) {
        this.connector = connector;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void connectRegisteredConnection(McpConnectionRegisteredEvent event) {
        connector.connect(event.connection());
    }
}
