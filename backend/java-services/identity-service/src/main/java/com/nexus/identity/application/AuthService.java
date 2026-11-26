package com.nexus.identity.application;

import com.nexus.common.error.ApiException;
import com.nexus.identity.config.IdentityProperties;
import com.nexus.identity.domain.AuthAuditEntry;
import com.nexus.identity.domain.RefreshToken;
import com.nexus.identity.domain.RequestMetadata;
import com.nexus.identity.domain.Tenant;
import com.nexus.identity.domain.UserAccount;
import com.nexus.identity.infrastructure.persistence.RefreshTokenRepository;
import com.nexus.identity.infrastructure.persistence.TenantRepository;
import com.nexus.identity.infrastructure.persistence.UserAccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Login, refresh-token rotation (with family reuse detection) and logout.
 * Methods use noRollbackFor so that security bookkeeping (failed attempts, family revocation, audit)
 * is committed even when the request is rejected.
 */
@Service
public class AuthService {
    /** Used to equalise timing when the user does not exist. Hash of a random string. */
    private static final String DUMMY_HASH =
            "{bcrypt}$2a$12$LwB/CNuV0A31njR2oDJOq.p8vwh34tqsQwdK025nX3I2xWQbH1t26";

    private final TenantRepository tenants;
    private final UserAccountRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder passwordEncoder;
    private final AccessTokenIssuer accessTokens;
    private final AuditRecorder audit;
    private final IdentityProperties props;
    private final Clock clock;

    public AuthService(TenantRepository tenants, UserAccountRepository users, RefreshTokenRepository refreshTokens,
                       PasswordEncoder passwordEncoder, AccessTokenIssuer accessTokens, AuditRecorder audit,
                       IdentityProperties props, Clock clock) {
        this.tenants = tenants;
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.passwordEncoder = passwordEncoder;
        this.accessTokens = accessTokens;
        this.audit = audit;
        this.props = props;
        this.clock = clock;
    }

    @Transactional(noRollbackFor = ApiException.class)
    public TokenPair login(String tenantSlug, String email, String password, RequestMetadata meta) {
        Instant now = clock.instant();
        Optional<Tenant> tenant = tenants.findBySlug(tenantSlug.trim().toLowerCase());
        Optional<UserAccount> found = tenant.filter(Tenant::isActive)
                .flatMap(t -> users.findByTenantIdAndEmailNormalized(t.getId(), UserAccount.normalizeEmail(email)));

        if (found.isEmpty()) {
            passwordEncoder.matches(password, DUMMY_HASH); // constant-ish time for unknown users
            audit.audit(AuthAuditEntry.Type.LOGIN_FAILED, tenant.map(Tenant::getId).orElse(null), null, meta,
                    Map.of("reason", "UNKNOWN_USER_OR_TENANT"), now);
            throw invalidCredentials();
        }

        UserAccount user = found.get();
        if (!user.isActive() || user.isLocked(now)) {
            passwordEncoder.matches(password, DUMMY_HASH);
            audit.audit(AuthAuditEntry.Type.LOGIN_FAILED, user.getTenantId(), user.getId(), meta,
                    Map.of("reason", user.isActive() ? "LOCKED" : "DISABLED"), now);
            throw invalidCredentials();
        }

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            boolean lockedNow = user.registerFailedLogin(props.maxFailedAttempts(), props.lockDuration(), now);
            audit.audit(lockedNow ? AuthAuditEntry.Type.ACCOUNT_LOCKED : AuthAuditEntry.Type.LOGIN_FAILED,
                    user.getTenantId(), user.getId(), meta, Map.of("reason", "BAD_PASSWORD"), now);
            throw invalidCredentials();
        }

        user.registerSuccessfulLogin(now);
        audit.audit(AuthAuditEntry.Type.LOGIN_SUCCEEDED, user.getTenantId(), user.getId(), meta, null, now);
        return issuePair(user, UUID.randomUUID(), meta, now).pair();
    }

    @Transactional(noRollbackFor = ApiException.class)
    public TokenPair refresh(String rawRefreshToken, RequestMetadata meta) {
        Instant now = clock.instant();
        RefreshToken current = refreshTokens.findByHashForUpdate(TokenHasher.sha256Hex(rawRefreshToken))
                .orElseThrow(AuthService::invalidRefresh);

        if (current.isRevoked()) {
            if (current.getRevokedReason() == RefreshToken.RevokeReason.ROTATED) {
                // A token that was already exchanged is being replayed: assume theft, kill the whole family.
                refreshTokens.revokeFamily(current.getFamilyId(), RefreshToken.RevokeReason.REUSE_DETECTED, now);
                audit.audit(AuthAuditEntry.Type.REFRESH_TOKEN_REUSE, current.getTenantId(), current.getUser().getId(),
                        meta, Map.of("familyId", current.getFamilyId().toString()), now);
                throw new ApiException(IdentityErrorCode.REFRESH_TOKEN_REUSED,
                        "This refresh token was already used. All sessions in this login have been revoked.");
            }
            throw invalidRefresh();
        }
        if (current.isExpired(now)) {
            throw invalidRefresh();
        }

        UserAccount user = current.getUser();
        Tenant tenant = tenants.findById(user.getTenantId()).orElseThrow(AuthService::invalidRefresh);
        if (!user.isActive() || !tenant.isActive()) {
            refreshTokens.revokeFamily(current.getFamilyId(), RefreshToken.RevokeReason.ACCOUNT_DISABLED, now);
            throw invalidRefresh();
        }

        Issued next = issuePair(user, current.getFamilyId(), meta, now);
        current.revoke(RefreshToken.RevokeReason.ROTATED, now, next.refreshTokenId());
        audit.audit(AuthAuditEntry.Type.TOKEN_REFRESHED, user.getTenantId(), user.getId(), meta, null, now);
        return next.pair();
    }

    /** Revokes the whole login family of the given refresh token. Idempotent: unknown tokens are ignored. */
    @Transactional
    public void logout(String rawRefreshToken, RequestMetadata meta) {
        Instant now = clock.instant();
        refreshTokens.findByHashForUpdate(TokenHasher.sha256Hex(rawRefreshToken)).ifPresent(t -> {
            refreshTokens.revokeFamily(t.getFamilyId(), RefreshToken.RevokeReason.LOGOUT, now);
            audit.audit(AuthAuditEntry.Type.LOGOUT, t.getTenantId(), t.getUser().getId(), meta, null, now);
        });
    }

    private Issued issuePair(UserAccount user, UUID familyId, RequestMetadata meta, Instant now) {
        AccessTokenIssuer.IssuedToken access = accessTokens.issue(user, now);
        String raw = TokenHasher.newOpaqueToken();
        Instant refreshExp = now.plus(props.refreshTokenTtl());
        RefreshToken stored = refreshTokens.save(RefreshToken.issue(user, familyId, TokenHasher.sha256Hex(raw), now,
                refreshExp, meta == null ? null : meta.ip(), meta == null ? null : meta.userAgent()));
        return new Issued(new TokenPair(access.value(), access.expiresAt(), raw, refreshExp), stored.getId());
    }

    private record Issued(TokenPair pair, UUID refreshTokenId) {}

    private static ApiException invalidCredentials() {
        return new ApiException(IdentityErrorCode.INVALID_CREDENTIALS, "Tenant, email or password is incorrect");
    }

    private static ApiException invalidRefresh() {
        return new ApiException(IdentityErrorCode.INVALID_REFRESH_TOKEN, "Refresh token is invalid or expired");
    }
}
