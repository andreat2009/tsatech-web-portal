package com.newproject.web.interceptor;

import com.newproject.web.dto.AdminAuditEventRequest;
import com.newproject.web.service.GatewayClient;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AdminAuditInterceptor implements HandlerInterceptor {
    private static final Set<String> MUTATING_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");
    private static final Set<String> STATE_CHANGE_SUFFIXES = Set.of("/status", "/close", "/approve", "/publish");
    private static final Set<String> IDENTIFIER_WORDS = Set.of("edit", "delete", "status", "close", "new", "modifica", "approve", "publish");

    private final GatewayClient gatewayClient;

    public AdminAuditInterceptor(GatewayClient gatewayClient) {
        this.gatewayClient = gatewayClient;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        if (!shouldAudit(request)) {
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken || !authentication.isAuthenticated() || !isAdmin(authentication)) {
            return;
        }

        try {
            AdminAuditEventRequest event = new AdminAuditEventRequest();
            event.setActorUsername(authentication.getName());
            event.setActorSubject(resolveSubject(authentication));
            event.setActionType(inferActionType(request));
            event.setTargetType(inferTargetType(request));
            event.setTargetId(extractTargetId(request.getRequestURI()));
            event.setRequestPath(request.getRequestURI());
            event.setHttpMethod(request.getMethod());
            event.setOutcome(resolveOutcome(response, ex));
            event.setStatusCode(response.getStatus());
            event.setSummary(buildSummary(event));
            event.setIpAddress(request.getRemoteAddr());
            event.setUserAgent(truncate(request.getHeader("User-Agent"), 1024));
            gatewayClient.recordAdminAuditEvent(event);
        } catch (Exception ignored) {
            // Audit logging must never break the admin flow.
        }
    }

    private boolean shouldAudit(HttpServletRequest request) {
        if (request == null || request.getRequestURI() == null || request.getMethod() == null) {
            return false;
        }
        String method = request.getMethod().toUpperCase(Locale.ROOT);
        if (!MUTATING_METHODS.contains(method)) {
            return false;
        }
        String uri = request.getRequestURI();
        return uri.startsWith("/admin") || uri.startsWith("/amministrazione");
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch(role -> "ROLE_ADMIN".equalsIgnoreCase(role));
    }

    private String resolveSubject(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof OidcUser oidcUser) {
            return truncate(oidcUser.getSubject(), 255);
        }
        return null;
    }

    private String inferActionType(HttpServletRequest request) {
        String method = request.getMethod().toUpperCase(Locale.ROOT);
        String uri = request.getRequestURI();
        if (uri.endsWith("/delete") || "DELETE".equals(method)) {
            return "DELETE";
        }
        if (STATE_CHANGE_SUFFIXES.stream().anyMatch(uri::endsWith)) {
            return "STATE_CHANGE";
        }
        if ("PUT".equals(method) || "PATCH".equals(method)) {
            return "UPDATE";
        }
        if ("POST".equals(method) && extractTargetId(uri) != null) {
            return "UPDATE";
        }
        if ("POST".equals(method)) {
            return "CREATE";
        }
        return method;
    }

    private String inferTargetType(HttpServletRequest request) {
        List<String> segments = Arrays.stream(request.getRequestURI().split("/"))
            .filter(segment -> segment != null && !segment.isBlank())
            .toList();
        if (segments.isEmpty()) {
            return "ADMIN_RESOURCE";
        }
        int offset = ("admin".equals(segments.get(0)) || "amministrazione".equals(segments.get(0))) ? 1 : 0;
        if (segments.size() <= offset) {
            return "ADMIN_RESOURCE";
        }
        String first = sanitizeSegment(segments.get(offset));
        String second = segments.size() > offset + 1 && !isIdentifierSegment(segments.get(offset + 1))
            ? sanitizeSegment(segments.get(offset + 1))
            : null;
        return second == null ? first : first + "_" + second;
    }

    private String extractTargetId(String uri) {
        String[] segments = uri.split("/");
        for (int i = segments.length - 1; i >= 0; i--) {
            String segment = segments[i];
            if (segment != null && segment.matches("\\d+")) {
                return segment;
            }
        }
        return null;
    }

    private String resolveOutcome(HttpServletResponse response, Exception ex) {
        if (ex != null) {
            return "FAILURE";
        }
        int status = response.getStatus();
        return status >= 200 && status < 400 ? "SUCCESS" : "FAILURE";
    }

    private String buildSummary(AdminAuditEventRequest event) {
        StringBuilder summary = new StringBuilder();
        summary.append(event.getActionType()).append(' ').append(event.getTargetType());
        if (event.getTargetId() != null && !event.getTargetId().isBlank()) {
            summary.append(" #").append(event.getTargetId());
        }
        summary.append(" via ").append(event.getHttpMethod()).append(' ').append(event.getRequestPath());
        return truncate(summary.toString(), 1024);
    }

    private boolean isIdentifierSegment(String value) {
        return value != null && (value.matches("\\d+") || IDENTIFIER_WORDS.contains(value.toLowerCase(Locale.ROOT)));
    }

    private String sanitizeSegment(String segment) {
        return segment.replace('-', '_').replace(' ', '_').toUpperCase(Locale.ROOT);
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
