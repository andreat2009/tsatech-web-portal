package com.newproject.web.service;

public class KeycloakRegistrationException extends RuntimeException {
    private final String reason;

    public KeycloakRegistrationException(String reason, String message) {
        super(message);
        this.reason = reason;
    }

    public KeycloakRegistrationException(String reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }
}
