package com.berdachuk.aichat.core.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class UserContext {

    public String getUserId() {
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
