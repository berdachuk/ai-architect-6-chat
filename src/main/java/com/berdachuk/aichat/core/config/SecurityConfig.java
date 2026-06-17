package com.berdachuk.aichat.core.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(AiChatSecurityProperties.class)
public class SecurityConfig {
  /**
   * Default: no OAuth2/JWT — open access for local and integration testing. User identity comes
   * from {@code X-User-Id} header or {@code aichat-user-id} cookie via {@link
   * com.berdachuk.aichat.core.security.UserContext}.
   */
  @Bean
  @ConditionalOnProperty(
      name = "ai-chat.security.oauth2-enabled",
      havingValue = "false",
      matchIfMissing = true)
  SecurityFilterChain devSecurityFilterChain(HttpSecurity http) throws Exception {
    return http
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(s -> s.sessionCreationPolicy(STATELESS))
        .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
        .httpBasic(AbstractHttpConfigurer::disable)
        .build();
  }

  /** Production: JWT required for REST API; web UI and static assets stay open for staged rollout. */
  @Bean
  @ConditionalOnProperty(name = "ai-chat.security.oauth2-enabled", havingValue = "true")
  SecurityFilterChain oauth2SecurityFilterChain(HttpSecurity http) throws Exception {
    return http
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(s -> s.sessionCreationPolicy(STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/api/v1/**")
                    .authenticated()
                    .requestMatchers(
                        "/",
                        "/chat/**",
                        "/css/**",
                        "/js/**",
                        "/v3/api-docs",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/actuator/health",
                        "/actuator/info")
                    .permitAll()
                    .anyRequest()
                    .permitAll())
        .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
        .httpBasic(AbstractHttpConfigurer::disable)
        .build();
  }
}
