package com.berdachuk.aichat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.server.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;

import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AiChatApplication implements ApplicationListener<WebServerInitializedEvent> {

    private static final Logger log = LoggerFactory.getLogger(AiChatApplication.class);

    private final Environment environment;

    public AiChatApplication(Environment environment) {
        this.environment = environment;
    }

    public static void main(String[] args) {
        SpringApplication.run(AiChatApplication.class, args);
    }

    @Override
    public void onApplicationEvent(WebServerInitializedEvent event) {
        int port = event.getWebServer().getPort();
        String address = environment.getProperty("server.address", "localhost");
        boolean sslEnabled = environment.getProperty("server.ssl.enabled", Boolean.class, false);
        String protocol = sslEnabled ? "https" : "http";
        String baseUrl = String.format("%s://%s:%d", protocol, address, port);

        log.info("");
        log.info("AiChat Application Started");
        log.info("Application URLs:");
        log.info("  UI:              {}", baseUrl + "/");
        log.info("  Health:          {}", baseUrl + "/actuator/health");
        log.info("  Swagger UI:      {}", baseUrl + "/swagger-ui.html");
        log.info("  OpenAPI JSON:    {}", baseUrl + "/v3/api-docs");
        log.info("");
    }
}
