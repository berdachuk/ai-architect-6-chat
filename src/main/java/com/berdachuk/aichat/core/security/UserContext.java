package com.berdachuk.aichat.core.security;

import com.berdachuk.aichat.core.config.AiChatSecurityProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class UserContext {

  private final AiChatSecurityProperties securityProperties;

  public UserContext(AiChatSecurityProperties securityProperties) {
    this.securityProperties = securityProperties;
  }

  public String getUserId() {
    if (securityProperties.oauth2Enabled()) {
      return resolveJwtUserId().orElseGet(this::resolveDevUserId);
    }
    return resolveDevUserId();
  }

  private java.util.Optional<String> resolveJwtUserId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication instanceof JwtAuthenticationToken jwtAuth) {
      Jwt jwt = jwtAuth.getToken();
      String claim = jwt.getClaimAsString(securityProperties.jwtUserClaim());
      if (claim != null && !claim.isBlank()) {
        return java.util.Optional.of(claim);
      }
      if (jwt.getSubject() != null && !jwt.getSubject().isBlank()) {
        return java.util.Optional.of(jwt.getSubject());
      }
    }
    return java.util.Optional.empty();
  }

  private String resolveDevUserId() {
    RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
    if (attrs instanceof ServletRequestAttributes servletAttrs) {
      HttpServletRequest request = servletAttrs.getRequest();
      String headerId = request.getHeader("X-User-Id");
      if (headerId != null && !headerId.isBlank()) {
        return headerId;
      }
      Cookie[] cookies = request.getCookies();
      if (cookies != null) {
        for (Cookie cookie : cookies) {
          if ("aichat-user-id".equals(cookie.getName())) {
            return cookie.getValue();
          }
        }
      }
    }
    return "anonymous";
  }
}
