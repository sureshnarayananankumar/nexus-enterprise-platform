package com.nexus.identity.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

    public enum RevokeReason { ROTATED, LOGOUT, REUSE_DETECTED, ACCOUNT_DISABLED }

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private UserAccount user;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "family_id", nullable = false, updatable = false)
    private UUID familyId;

    @Column(name = "token_hash", nullable = false, length = 64, unique = true)
    private String tokenHash;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "revoked_reason", length = 32)
    private RevokeReason revokedReason;

    @Column(name = "replaced_by_id")
    private UUID replacedById;

    @Column(name = "created_ip", length = 45)
    private String createdIp;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    protected RefreshToken() {}

    public static RefreshToken issue(UserAccount user, UUID familyId, String tokenHash, Instant now, Instant expiresAt,
                                     String ip, String userAgent) {
        RefreshToken t = new RefreshToken();
        t.id = UUID.randomUUID();
        t.user = user;
        t.tenantId = user.getTenantId();
        t.familyId = familyId;
        t.tokenHash = tokenHash;
        t.issuedAt = now;
        t.expiresAt = expiresAt;
        t.createdIp = ip;
        t.userAgent = userAgent == null ? null : userAgent.substring(0, Math.min(255, userAgent.length()));
        return t;
    }

    public boolean isRevoked() { return revokedAt != null; }

    public boolean isExpired(Instant now) { return !expiresAt.isAfter(now); }

    public void revoke(RevokeReason reason, Instant now, UUID replacedBy) {
        this.revokedAt = now;
        this.revokedReason = reason;
        this.replacedById = replacedBy;
    }

    public UUID getId() { return id; }
    public UserAccount getUser() { return user; }
    public UUID getTenantId() { return tenantId; }
    public UUID getFamilyId() { return familyId; }
    public RevokeReason getRevokedReason() { return revokedReason; }
    public Instant getExpiresAt() { return expiresAt; }
}
