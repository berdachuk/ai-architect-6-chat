package com.berdachuk.aichat.core.security;

import com.berdachuk.aichat.core.config.AiChatSecurityProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UserContextTest {

  @AfterEach
  void clearContext() {
    SecurityContextHolder.clearContext();
    RequestContextHolder.resetRequestAttributes();
  }

  @Test
  void devModeUsesHeaderThenCookieThenAnonymous() {
    UserContext context = new UserContext(new AiChatSecurityProperties(false, "sub"));

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("X-User-Id", "header-user");
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    assertThat(context.getUserId()).isEqualTo("header-user");

    request = new MockHttpServletRequest();
    request.setCookies(new jakarta.servlet.http.Cookie("aichat-user-id", "cookie-user"));
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    assertThat(context.getUserId()).isEqualTo("cookie-user");

    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
    assertThat(context.getUserId()).isEqualTo("anonymous");
  }

  @Test
  void oauthModeUsesJwtClaim() {
    UserContext context = new UserContext(new AiChatSecurityProperties(true, "sub"));
    Jwt jwt =
        Jwt.withTokenValue("token")
            .header("alg", "none")
            .subject("jwt-subject")
            .claim("sub", "jwt-subject")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .build();
    SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));

    assertThat(context.getUserId()).isEqualTo("jwt-subject");
  }

  @Test
  void oauthModeFallsBackToDevHeaderWithoutJwt() {
    UserContext context = new UserContext(new AiChatSecurityProperties(true, "sub"));
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("X-User-Id", "header-user");
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

    assertThat(context.getUserId()).isEqualTo("header-user");
  }
}
