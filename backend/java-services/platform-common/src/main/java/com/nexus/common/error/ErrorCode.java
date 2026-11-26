package com.nexus.common.error;

import org.springframework.http.HttpStatus;

/** Platform-wide error codes. Services may add their own codes by implementing {@link ApiErrorCode}. */
public enum ErrorCode implements ApiErrorCode {
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Validation failed"),
    MALFORMED_REQUEST(HttpStatus.BAD_REQUEST, "Malformed request"),
    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "Authentication required"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "Access denied"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "Resource not found"),
    CONFLICT(HttpStatus.CONFLICT, "Conflict"),
    PRECONDITION_FAILED(HttpStatus.PRECONDITION_FAILED, "Precondition failed"),
    RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "Too many requests"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");

    private final HttpStatus status;
    private final String title;

    ErrorCode(HttpStatus status, String title) {
        this.status = status;
        this.title = title;
    }

    @Override public HttpStatus status() { return status; }
    @Override public String title() { return title; }
    @Override public String code() { return name(); }
}
