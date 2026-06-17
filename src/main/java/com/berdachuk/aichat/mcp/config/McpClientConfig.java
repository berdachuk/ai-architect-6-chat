package com.berdachuk.aichat.mcp.config;

import com.berdachuk.aichat.core.config.AiChatFeatureProperties;
import com.berdachuk.aichat.mcp.config.McpBootstrapProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!test")
@ConditionalOnProperty(name = "ai-chat.features.mcp-client", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties({AiChatFeatureProperties.class, McpBootstrapProperties.class})
public class McpClientConfig {
}
