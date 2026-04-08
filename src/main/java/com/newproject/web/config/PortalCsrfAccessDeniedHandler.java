package com.newproject.web.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.InvalidCsrfTokenException;
import org.springframework.security.web.csrf.MissingCsrfTokenException;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

public class PortalCsrfAccessDeniedHandler implements AccessDeniedHandler {

    private final CsrfTokenRepository csrfTokenRepository;
    private final MessageSource messageSource;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PortalCsrfAccessDeniedHandler(CsrfTokenRepository csrfTokenRepository, MessageSource messageSource) {
        this.csrfTokenRepository = csrfTokenRepository;
        this.messageSource = messageSource;
    }

    @Override
    public void handle(
        HttpServletRequest request,
        HttpServletResponse response,
        AccessDeniedException accessDeniedException
    ) throws IOException, ServletException {
        Locale locale = LocaleContextHolder.getLocale();
        if (isCsrfException(accessDeniedException)) {
            refreshToken(request, response);
            String message = messageSource.getMessage(
                "common.error.csrf.expired",
                null,
                "Your session security token expired. Please try again.",
                locale
            );
            if (expectsJson(request)) {
                Map<String, Object> body = new LinkedHashMap<>();
                body.put("timestamp", OffsetDateTime.now().toString());
                body.put("status", HttpServletResponse.SC_FORBIDDEN);
                body.put("error", "Forbidden");
                body.put("code", "CSRF_EXPIRED");
                body.put("message", message);
                body.put("path", request.getRequestURI());
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                objectMapper.writeValue(response.getWriter(), body);
                return;
            }

            String redirectTarget = appendCsrfNotice(resolveRedirectTarget(request));
            response.setStatus(HttpServletResponse.SC_FOUND);
            response.setHeader(HttpHeaders.LOCATION, redirectTarget);
            return;
        }

        response.sendError(
            HttpServletResponse.SC_FORBIDDEN,
            messageSource.getMessage(
                "common.error.forbidden",
                null,
                "Insufficient permissions for this action.",
                locale
            )
        );
    }

    private boolean isCsrfException(AccessDeniedException accessDeniedException) {
        return accessDeniedException instanceof MissingCsrfTokenException
            || accessDeniedException instanceof InvalidCsrfTokenException;
    }

    private void refreshToken(HttpServletRequest request, HttpServletResponse response) {
        csrfTokenRepository.saveToken(null, request, response);
        CsrfToken freshToken = csrfTokenRepository.generateToken(request);
        csrfTokenRepository.saveToken(freshToken, request, response);
        request.setAttribute(CsrfToken.class.getName(), freshToken);
        request.setAttribute(freshToken.getParameterName(), freshToken);
    }

    private boolean expectsJson(HttpServletRequest request) {
        String accept = Objects.toString(request.getHeader(HttpHeaders.ACCEPT), "");
        String requestedWith = Objects.toString(request.getHeader("X-Requested-With"), "");
        String uri = Objects.toString(request.getRequestURI(), "");
        return uri.startsWith("/api/")
            || accept.contains(MediaType.APPLICATION_JSON_VALUE)
            || "XMLHttpRequest".equalsIgnoreCase(requestedWith);
    }

    private String resolveRedirectTarget(HttpServletRequest request) {
        String referer = request.getHeader(HttpHeaders.REFERER);
        if (StringUtils.hasText(referer)) {
            try {
                URI refererUri = URI.create(referer);
                if (isSameOrigin(request, refererUri) && StringUtils.hasText(refererUri.getRawPath())) {
                    String basePath = refererUri.getRawPath();
                    if (StringUtils.hasText(refererUri.getRawQuery())) {
                        return basePath + "?" + refererUri.getRawQuery();
                    }
                    return basePath;
                }
            } catch (IllegalArgumentException ignored) {
                // fallback below
            }
        }

        if (StringUtils.hasText(request.getRequestURI()) && !"POST".equalsIgnoreCase(request.getMethod())) {
            return request.getRequestURI();
        }
        return "/";
    }

    private boolean isSameOrigin(HttpServletRequest request, URI refererUri) {
        if (!refererUri.isAbsolute()) {
            return true;
        }
        int requestPort = normalizePort(request.getServerPort(), request.getScheme());
        int refererPort = normalizePort(refererUri.getPort(), refererUri.getScheme());
        return Objects.equals(request.getScheme(), refererUri.getScheme())
            && Objects.equals(request.getServerName(), refererUri.getHost())
            && requestPort == refererPort;
    }

    private int normalizePort(int port, String scheme) {
        if (port > 0) {
            return port;
        }
        if ("https".equalsIgnoreCase(scheme)) {
            return 443;
        }
        if ("http".equalsIgnoreCase(scheme)) {
            return 80;
        }
        return port;
    }

    private String appendCsrfNotice(String target) {
        return UriComponentsBuilder.fromUriString(target)
            .replaceQueryParam("csrf")
            .queryParam("csrf", "expired")
            .build(true)
            .toUriString();
    }
}
