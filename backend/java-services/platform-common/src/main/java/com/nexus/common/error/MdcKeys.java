package com.nexus.common.error;

/** MDC keys used in structured logs. */
public final class MdcKeys {
    public static final String CORRELATION_ID = "traceId";
    public static final String TENANT_ID = "tenantId";
    public static final String USER_ID = "userId";

    private MdcKeys() {}
}
