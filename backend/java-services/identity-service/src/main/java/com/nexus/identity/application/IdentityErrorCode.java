package com.nexus.identity.application;

import com.nexus.common.error.ApiErrorCode;
import org.springframework.http.HttpStatus;

/** identity-service specific error codes (rendered as RFC 9457 problem details). */
public enum IdentityErrorCode implements ApiErrorCode {
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Invalid credentials"),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "Invalid refresh token"),
    REFRESH_TOKEN_REUSED(HttpStatus.UNAUTHORIZED, "Refresh token reuse detected"),
    TENANT_SLUG_TAKEN(HttpStatus.CONFLICT, "Tenant slug already in use"),
    EMAIL_TAKEN(HttpStatus.CONFLICT, "Email already registered in this tenant"),
    UNKNOWN_ROLE(HttpStatus.BAD_REQUEST, "Unknown role"),
    CANNOT_CHANGE_OWN_STATUS(HttpStatus.CONFLICT, "Cannot change your own account status");

    private final HttpStatus status;
    private final String title;

    IdentityErrorCode(HttpStatus status, String title) {
        this.status = status;
        this.title = title;
    }

    @Override public String code() { return name(); }
    @Override public HttpStatus status() { return status; }
    @Override public String title() { return title; }
}
