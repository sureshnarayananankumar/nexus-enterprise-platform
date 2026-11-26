package com.nexus.common.error;

import org.springframework.http.HttpStatus;

/** Contract for error codes rendered as RFC 9457 problem details. */
public interface ApiErrorCode {
    String code();
    HttpStatus status();
    String title();
}
