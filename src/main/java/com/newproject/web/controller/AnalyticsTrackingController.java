package com.newproject.web.controller;

import com.newproject.web.dto.AnalyticsEventRequest;
import com.newproject.web.service.GatewayClient;
import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/analytics")
public class AnalyticsTrackingController {
    private static final String CONSENT_COOKIE = "tsa_cookie_consent";

    private final GatewayClient gatewayClient;

    public AnalyticsTrackingController(GatewayClient gatewayClient) {
        this.gatewayClient = gatewayClient;
    }

    @PostMapping("/track")
    public ResponseEntity<Void> track(
        @RequestBody AnalyticsEventRequest payload,
        @CookieValue(name = CONSENT_COOKIE, required = false) String consent,
        @CookieValue(name = "tsa_vid", required = false) String visitorCookie,
        HttpServletRequest request,
        Principal principal
    ) {
        if (!"accepted".equalsIgnoreCase(consent)) {
            return ResponseEntity.accepted().build();
        }

        String path = trimToNull(payload != null ? payload.getPath() : null);
        if (path == null || path.startsWith("/admin") || path.startsWith("/amministrazione")) {
            return ResponseEntity.noContent().build();
        }

        AnalyticsEventRequest event = new AnalyticsEventRequest();
        event.setVisitorId(trimToNull(visitorCookie) != null ? trimToNull(visitorCookie) : UUID.randomUUID().toString());
        event.setEventType(trimToNull(payload != null ? payload.getEventType() : null));
        event.setPath(path);
        event.setPageTitle(trimToNull(payload != null ? payload.getPageTitle() : null));
        event.setReferrer(trimToNull(payload != null ? payload.getReferrer() : null));
        event.setEntityType(trimToNull(payload != null ? payload.getEntityType() : null));
        event.setEntityId(trimToNull(payload != null ? payload.getEntityId() : null));
        event.setLocale(trimToNull(payload != null ? payload.getLocale() : null));
        event.setIp(resolveClientIp(request));
        event.setUserAgent(trimToNull(request.getHeader("User-Agent")));
        event.setUserId(principal != null ? principal.getName() : null);

        gatewayClient.trackAnalyticsEvent(event);
        return ResponseEntity.accepted().build();
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = trimToNull(request.getHeader("X-Forwarded-For"));
        if (forwarded != null) {
            String first = forwarded.split(",")[0];
            return trimToNull(first);
        }
        return trimToNull(request.getRemoteAddr());
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
