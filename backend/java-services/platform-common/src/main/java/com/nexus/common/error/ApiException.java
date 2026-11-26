package com.nexus.common.error;

/** Business/application exception carrying a stable error code. */
public class ApiException extends RuntimeException {
    private final transient ApiErrorCode errorCode;

    public ApiException(ApiErrorCode errorCode, String detail) {
        super(detail);
        this.errorCode = errorCode;
    }

    public ApiErrorCode getErrorCode() { return errorCode; }

    public static ApiException notFound(String what) {
        return new ApiException(ErrorCode.NOT_FOUND, what + " not found");
    }

    public static ApiException conflict(String detail) {
        return new ApiException(ErrorCode.CONFLICT, detail);
    }

    public static ApiException badRequest(String detail) {
        return new ApiException(ErrorCode.VALIDATION_ERROR, detail);
    }
}
