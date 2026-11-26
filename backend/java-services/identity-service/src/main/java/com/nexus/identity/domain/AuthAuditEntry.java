package com.nexus.identity.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** Append-only record of security-relevant authentication events. */
@Entity
@Table(name = "auth_audit_log")
public class AuthAuditEntry {

    public enum Type {
        TENANT_CREATED, LOGIN_SUCCEEDED, LOGIN_FAILED, ACCOUNT_LOCKED, TOKEN_REFRESHED,
        REFRESH_TOKEN_REUSE, LOGOUT, ACCOUNT_CREATED, ACCOUNT_STATUS_CHANGED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "user_id")
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 40)
    private Type eventType;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> details;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected AuthAuditEntry() {}

    public static AuthAuditEntry of(Type type, UUID tenantId, UUID userId, RequestMetadata meta,
                                    Map<String, Object> details, Instant now) {
        AuthAuditEntry e = new AuthAuditEntry();
        e.eventType = type;
        e.tenantId = tenantId;
        e.userId = userId;
        e.ipAddress = meta == null ? null : meta.ip();
        e.userAgent = meta == null ? null : meta.userAgentTruncated();
        e.details = details;
        e.occurredAt = now;
        return e;
    }

    public Type getEventType() { return eventType; }
    public UUID getTenantId() { return tenantId; }
    public UUID getUserId() { return userId; }
}
