package com.berdachuk.aichat.core.security;

import com.berdachuk.aichat.core.config.AiChatSecurityProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class UserContext {

  private static final Logger log = LoggerFactory.getLogger(UserContext.class);
  private static final int MAX_USER_ID_LENGTH = 100;
  private static final String USER_ID_PATTERN = "[a-zA-Z0-9._\\-@]+";

  private final AiChatSecurityProperties securityProperties;

  public UserContext(AiChatSecurityProperties securityProperties) {
    this.securityProperties = securityProperties;
  }

  public String getUserId() {
    if (securityProperties.oauth2LoginEnabled()) {
      return resolveOAuth2UserId().orElse("anonymous");
    }
    if (securityProperties.oauth2Enabled()) {
      return resolveJwtUserId().orElseGet(this::resolveDevUserId);
    }
    return resolveDevUserId();
  }

  public boolean isOAuth2LoginEnabled() {
    return securityProperties.oauth2LoginEnabled();
  }

  private Optional<String> resolveJwtUserId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication instanceof JwtAuthenticationToken jwtAuth) {
      Jwt jwt = jwtAuth.getToken();
      String claim = jwt.getClaimAsString(securityProperties.jwtUserClaim());
      if (claim != null && !claim.isBlank()) {
        return Optional.of(claim);
      }
      if (jwt.getSubject() != null && !jwt.getSubject().isBlank()) {
        return Optional.of(jwt.getSubject());
      }
    }
    return Optional.empty();
  }

  private Optional<String> resolveOAuth2UserId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      return Optional.empty();
    }
    Object principal = authentication.getPrincipal();
    if (principal instanceof OidcUser oidcUser) {
      String subject = oidcUser.getSubject();
      if (subject != null && !subject.isBlank()) {
        return Optional.of(subject);
      }
    }
    if (principal instanceof OAuth2User oauth2User) {
      Object claim = oauth2User.getAttribute(securityProperties.jwtUserClaim());
      if (claim != null && !claim.toString().isBlank()) {
        return Optional.of(claim.toString());
      }
        oauth2User.getName();
        if (!oauth2User.getName().isBlank()) {
        return Optional.of(oauth2User.getName());
      }
    }
    return Optional.empty();
  }

  private String resolveDevUserId() {
    RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
    if (attrs instanceof ServletRequestAttributes servletAttrs) {
      HttpServletRequest request = servletAttrs.getRequest();
      String headerId = request.getHeader("X-User-Id");
      if (headerId != null && !headerId.isBlank()) {
        if (headerId.length() > MAX_USER_ID_LENGTH || !headerId.matches(USER_ID_PATTERN)) {
          log.warn("Rejected invalid X-User-Id header: length={}, matches pattern={}",
              headerId.length(), headerId.matches(USER_ID_PATTERN));
          return "anonymous";
        }
        return headerId;
      }
      Cookie[] cookies = request.getCookies();
      if (cookies != null) {
        for (Cookie cookie : cookies) {
          if ("aichat-user-id".equals(cookie.getName())) {
            String cookieValue = cookie.getValue();
            if (cookieValue != null && !cookieValue.isBlank()
                && cookieValue.length() <= MAX_USER_ID_LENGTH
                && cookieValue.matches(USER_ID_PATTERN)) {
              return cookieValue;
            }
          }
        }
      }
    }
    return "anonymous";
  }
}
