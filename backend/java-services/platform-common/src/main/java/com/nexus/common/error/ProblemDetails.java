package com.nexus.common.error;

import org.slf4j.MDC;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;

import java.net.URI;
import java.util.List;
import java.util.Map;

/** Factory for consistent RFC 9457 problem documents across services. */
public final class ProblemDetails {
    public static final String TYPE_BASE = "https://nexus.dev/problems/";

    private ProblemDetails() {}

    public static ProblemDetail of(ApiErrorCode code, String detail, String instance) {
        return of(code.status(), code.code(), code.title(), detail, instance);
    }

    public static ProblemDetail of(HttpStatusCode status, String code, String title, String detail, String instance) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setType(URI.create(TYPE_BASE + code.toLowerCase().replace('_', '-')));
        pd.setTitle(title);
        if (instance != null) pd.setInstance(URI.create(instance));
        pd.setProperty("code", code);
        String traceId = MDC.get(MdcKeys.CORRELATION_ID);
        if (traceId != null) pd.setProperty("traceId", traceId);
        return pd;
    }

    public static ProblemDetail withFieldErrors(ProblemDetail pd, List<Map<String, String>> errors) {
        pd.setProperty("errors", errors);
        return pd;
    }
}
