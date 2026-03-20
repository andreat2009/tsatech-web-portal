package com.newproject.web.service;

import com.newproject.web.dto.CustomerRegistrationForm;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Service
public class KeycloakRegistrationService {
    private static final Logger logger = LoggerFactory.getLogger(KeycloakRegistrationService.class);

    private final WebClient webClient;
    private final String tokenEndpoint;
    private final String adminRealmEndpoint;
    private final String adminClientId;
    private final String adminUsername;
    private final String adminPassword;
    private final String registrationRole;

    public KeycloakRegistrationService(
        @Qualifier("defaultWebClient") WebClient webClient,
        @Value("${spring.security.oauth2.client.provider.keycloak.issuer-uri}") String issuerUri,
        @Value("${app.keycloak-admin-base-url:}") String adminBaseUrl,
        @Value("${app.keycloak-admin-client-id:admin-cli}") String adminClientId,
        @Value("${app.keycloak-admin-username:admin}") String adminUsername,
        @Value("${app.keycloak-admin-password:adminPass}") String adminPassword,
        @Value("${app.keycloak-registration-role:USER}") String registrationRole
    ) {
        this.webClient = webClient;
        this.adminClientId = adminClientId;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
        this.registrationRole = registrationRole;

        String normalizedIssuer = normalize(issuerUri);
        int markerIdx = normalizedIssuer.indexOf("/realms/");
        if (markerIdx < 0) {
            throw new IllegalArgumentException("Invalid Keycloak issuer URI: " + issuerUri);
        }

        String realm = normalizedIssuer.substring(markerIdx + "/realms/".length());
        int slash = realm.indexOf('/');
        if (slash >= 0) {
            realm = realm.substring(0, slash);
        }

        String base = isBlank(adminBaseUrl) ? normalizedIssuer.substring(0, markerIdx) : normalize(adminBaseUrl);

        this.tokenEndpoint = base + "/realms/" + realm + "/protocol/openid-connect/token";
        this.adminRealmEndpoint = base + "/admin/realms/" + realm;
    }

    public String createUserWithRole(CustomerRegistrationForm form) {
        String accessToken = obtainAdminAccessToken();
        String email = normalizeEmail(form.getEmail());
        String username = email;

        String existing = findUserId(accessToken, username, email);
        if (existing != null) {
            throw new KeycloakRegistrationException("exists", "Keycloak user already exists");
        }

        String createdUserId = null;
        try {
            createdUserId = createUser(accessToken, form, username, email);
            if (createdUserId == null) {
                throw new KeycloakRegistrationException("provider", "Keycloak user id not available after creation");
            }

            setPassword(accessToken, createdUserId, form.getPassword());
            assignRealmRole(accessToken, createdUserId, registrationRole);
            return createdUserId;
        } catch (KeycloakRegistrationException ex) {
            if (createdUserId != null && !"exists".equals(ex.getReason())) {
                deleteUserQuietly(createdUserId);
            }
            throw ex;
        } catch (Exception ex) {
            if (createdUserId != null) {
                deleteUserQuietly(createdUserId);
            }
            throw new KeycloakRegistrationException("provider", "Keycloak registration failed", ex);
        }
    }

    public void deleteUserQuietly(String userId) {
        if (userId == null || userId.isBlank()) {
            return;
        }

        try {
            String accessToken = obtainAdminAccessToken();
            webClient.delete()
                .uri(adminRealmEndpoint + "/users/" + url(userId))
                .headers(h -> h.setBearerAuth(accessToken))
                .retrieve()
                .toBodilessEntity()
                .block();
        } catch (Exception ex) {
            logger.warn("Unable to rollback Keycloak user {}: {}", userId, ex.getMessage());
        }
    }

    private String createUser(String accessToken, CustomerRegistrationForm form, String username, String email) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("username", username);
        payload.put("email", email);
        String firstName = trimToNull(form.getFirstName());
        if (firstName != null) {
            payload.put("firstName", firstName);
        }
        String lastName = trimToNull(form.getLastName());
        if (lastName != null) {
            payload.put("lastName", lastName);
        }
        payload.put("enabled", true);
        payload.put("emailVerified", true);

        try {
            ResponseEntity<Void> response = webClient.post()
                .uri(adminRealmEndpoint + "/users")
                .contentType(MediaType.APPLICATION_JSON)
                .headers(h -> h.setBearerAuth(accessToken))
                .bodyValue(payload)
                .retrieve()
                .toBodilessEntity()
                .block();

            String userId = extractUserIdFromLocation(response != null ? response.getHeaders().getLocation() : null);
            if (userId != null) {
                return userId;
            }

            // fallback: eventual consistency on listing can lag briefly
            for (int i = 0; i < 6; i++) {
                String found = findUserId(accessToken, username, email);
                if (found != null) {
                    return found;
                }
                try {
                    Thread.sleep(200L);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
            return null;
        } catch (WebClientResponseException ex) {
            logHttpError("createUser", ex);
            if (ex.getStatusCode().value() == 409) {
                throw new KeycloakRegistrationException("exists", "Keycloak user already exists", ex);
            }
            throw new KeycloakRegistrationException("identity", "Failed to create Keycloak user", ex);
        }
    }

    private void setPassword(String accessToken, String userId, String password) {
        Map<String, Object> payload = Map.of(
            "type", "password",
            "temporary", false,
            "value", password
        );

        try {
            webClient.put()
                .uri(adminRealmEndpoint + "/users/" + url(userId) + "/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .headers(h -> h.setBearerAuth(accessToken))
                .bodyValue(payload)
                .retrieve()
                .toBodilessEntity()
                .block();
        } catch (WebClientResponseException ex) {
            logHttpError("setPassword", ex);
            if (ex.getStatusCode().is4xxClientError()) {
                throw new KeycloakRegistrationException("password_policy", "Keycloak password policy rejected password", ex);
            }
            throw new KeycloakRegistrationException("identity", "Failed to set Keycloak password", ex);
        }
    }

    private void assignRealmRole(String accessToken, String userId, String roleName) {
        try {
            Map<String, Object> roleRepresentation = webClient.get()
                .uri(adminRealmEndpoint + "/roles/" + url(roleName))
                .headers(h -> h.setBearerAuth(accessToken))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();

            if (roleRepresentation == null || roleRepresentation.isEmpty()) {
                throw new KeycloakRegistrationException("identity", "Keycloak role not found: " + roleName);
            }

            webClient.post()
                .uri(adminRealmEndpoint + "/users/" + url(userId) + "/role-mappings/realm")
                .contentType(MediaType.APPLICATION_JSON)
                .headers(h -> h.setBearerAuth(accessToken))
                .bodyValue(List.of(roleRepresentation))
                .retrieve()
                .toBodilessEntity()
                .block();
        } catch (WebClientResponseException ex) {
            logHttpError("assignRealmRole", ex);
            throw new KeycloakRegistrationException("identity", "Failed to assign Keycloak role", ex);
        }
    }

    private String extractUserIdFromLocation(URI location) {
        if (location == null) {
            return null;
        }
        String path = location.getPath();
        if (path == null || path.isBlank()) {
            return null;
        }
        int idx = path.lastIndexOf('/');
        if (idx < 0 || idx == path.length() - 1) {
            return null;
        }
        String candidate = path.substring(idx + 1).trim();
        return candidate.isBlank() ? null : candidate;
    }

    private String findUserId(String accessToken, String username, String email) {
        String byUsername = findUserIdByQuery(accessToken, "username", username);
        if (byUsername != null) {
            return byUsername;
        }
        return findUserIdByQuery(accessToken, "email", email);
    }

    private String findUserIdByQuery(String accessToken, String key, String value) {
        List<Map<String, Object>> users = webClient.get()
            .uri(adminRealmEndpoint + "/users?" + key + "=" + url(value))
            .headers(h -> h.setBearerAuth(accessToken))
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
            .block();

        if (users == null || users.isEmpty()) {
            return null;
        }

        for (Map<String, Object> candidate : users) {
            Object val = candidate.get(key);
            if (val != null && value.equalsIgnoreCase(val.toString())) {
                Object id = candidate.get("id");
                if (id != null) {
                    return id.toString();
                }
            }
        }

        Object fallbackId = users.get(0).get("id");
        return fallbackId != null ? fallbackId.toString() : null;
    }

    private String obtainAdminAccessToken() {
        if (isBlank(adminClientId) || isBlank(adminUsername) || isBlank(adminPassword)) {
            throw new KeycloakRegistrationException("identity", "Missing Keycloak admin credentials");
        }

        try {
            Map<String, Object> response = webClient.post()
                .uri(tokenEndpoint)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("grant_type", "password")
                    .with("client_id", adminClientId)
                    .with("username", adminUsername)
                    .with("password", adminPassword))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();

            Object token = response != null ? response.get("access_token") : null;
            if (token == null || token.toString().isBlank()) {
                throw new KeycloakRegistrationException("identity", "Unable to obtain Keycloak admin token");
            }
            return token.toString();
        } catch (WebClientResponseException ex) {
            logHttpError("obtainAdminAccessToken", ex);
            throw new KeycloakRegistrationException("identity", "Unable to obtain Keycloak admin token", ex);
        }
    }

    private void logHttpError(String step, WebClientResponseException ex) {
        logger.warn("Keycloak registration step '{}' failed: status={}, body={}", step, ex.getStatusCode().value(), ex.getResponseBodyAsString());
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private String normalizeEmail(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private String url(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
