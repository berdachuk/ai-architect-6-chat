package com.berdachuk.aichat.core.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI aiChatOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("AI Chat API")
                        .version("v1")
                        .description("Chat session CRUD and history"))
                .addSecurityItem(new SecurityRequirement().addList("X-User-Id"))
                .schemaRequirement("X-User-Id", new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.HEADER)
                        .name("X-User-Id"));
    }
}
