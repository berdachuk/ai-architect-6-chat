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

import static org.springframework.security.config.http.SessionCreationPolicy.IF_REQUIRED;
import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(AiChatSecurityProperties.class)
public class SecurityConfig {

  private static final String[] STATIC_AND_HEALTH = {
      "/css/**",
      "/js/**",
      "/actuator/health",
      "/actuator/info",
      "/v3/api-docs",
      "/v3/api-docs/**",
      "/swagger-ui/**",
      "/swagger-ui.html"
  };

  @Bean
  @ConditionalOnProperty(
      name = "ai-chat.security.oauth2-enabled",
      havingValue = "false",
      matchIfMissing = true)
  @ConditionalOnProperty(
      name = "ai-chat.security.oauth2-login-enabled",
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

  @Bean
  @ConditionalOnProperty(name = "ai-chat.security.oauth2-enabled", havingValue = "true")
  @ConditionalOnProperty(
      name = "ai-chat.security.oauth2-login-enabled",
      havingValue = "false",
      matchIfMissing = true)
  SecurityFilterChain oauth2SecurityFilterChain(HttpSecurity http) throws Exception {
    return http
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(s -> s.sessionCreationPolicy(STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/api/v1/**")
                    .authenticated()
                    .requestMatchers("/", "/chat/**")
                    .permitAll()
                    .requestMatchers(STATIC_AND_HEALTH)
                    .permitAll()
                    .anyRequest()
                    .permitAll())
        .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
        .httpBasic(AbstractHttpConfigurer::disable)
        .build();
  }

  @Bean
  @ConditionalOnProperty(name = "ai-chat.security.oauth2-login-enabled", havingValue = "true")
  SecurityFilterChain oauth2LoginSecurityFilterChain(HttpSecurity http) throws Exception {
    return http
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(s -> s.sessionCreationPolicy(IF_REQUIRED))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(STATIC_AND_HEALTH)
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .oauth2Login(Customizer.withDefaults())
        .oauth2Client(Customizer.withDefaults())
        .httpBasic(AbstractHttpConfigurer::disable)
        .build();
  }
}
