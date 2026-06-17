package com.berdachuk.aichat.mcp.service;

import com.berdachuk.aichat.mcp.domain.McpConnection;
import com.berdachuk.aichat.mcp.domain.ServerStatus;
import com.berdachuk.aichat.mcp.registry.McpServerRegistry;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
@WireMockTest
class McpClientIntegrationTest {

    @Test
    void registersDownConnectionWhenServerUnavailable(WireMockRuntimeInfo wm) {
        McpServerRegistry registry = new McpServerRegistry();
        McpClientConnector connector = new McpClientConnector(registry);
        Instant now = Instant.now();
        McpConnection connection = new McpConnection(
                "000000000000000000000099",
                "wiremock-server",
                wm.getHttpBaseUrl() + "/sse",
                true,
                false,
                false,
                now,
                now);

        connector.connect(connection);

        assertThat(registry.findById(connection.id())).isPresent();
        assertThat(registry.findById(connection.id()).orElseThrow().status()).isEqualTo(ServerStatus.DOWN);
    }

    @Test
    void resolveTransportTargetSplitsSseSuffix() {
        McpClientConnector.TransportTarget target =
                McpClientConnector.resolveTransportTarget("http://localhost:8080/custom/sse");
        assertThat(target.baseUrl()).isEqualTo("http://localhost:8080/custom");
        assertThat(target.sseEndpoint()).isEqualTo("/sse");
    }

    @Test
    void wireMockMessageEndpointCanBeStubbed(WireMockRuntimeInfo wm) throws Exception {
        stubFor(post(urlEqualTo("/mcp/message"))
                .withRequestBody(containing("tools/list"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "jsonrpc": "2.0",
                                  "id": 1,
                                  "result": {
                                    "tools": [
                                      {
                                        "name": "echo",
                                        "description": "Echo tool",
                                        "inputSchema": {"type": "object"}
                                      }
                                    ]
                                  }
                                }
                                """)));

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(HttpRequest.newBuilder()
                        .uri(URI.create(wm.getHttpBaseUrl() + "/mcp/message"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(
                                "{\"jsonrpc\":\"2.0\",\"method\":\"tools/list\",\"id\":1}"))
                        .build(), HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("echo");
        verify(postRequestedFor(urlEqualTo("/mcp/message")));
    }
}
