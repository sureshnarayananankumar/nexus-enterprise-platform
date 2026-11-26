package com.nexus.identity.application;

import com.nexus.common.error.ApiException;
import com.nexus.identity.domain.AuthAuditEntry;
import com.nexus.identity.domain.RefreshToken;
import com.nexus.identity.domain.RequestMetadata;
import com.nexus.identity.domain.Role;
import com.nexus.identity.domain.UserAccount;
import com.nexus.identity.infrastructure.persistence.RefreshTokenRepository;
import com.nexus.identity.infrastructure.persistence.RoleRepository;
import com.nexus.identity.infrastructure.persistence.UserAccountRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

/** Tenant-scoped account administration. Every method takes the caller's tenantId from the JWT. */
@Service
public class AccountService {
    private final UserAccountRepository users;
    private final RoleRepository roles;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder passwordEncoder;
    private final AuditRecorder audit;
    private final Clock clock;

    public AccountService(UserAccountRepository users, RoleRepository roles, RefreshTokenRepository refreshTokens,
                          PasswordEncoder passwordEncoder, AuditRecorder audit, Clock clock) {
        this.users = users;
        this.roles = roles;
        this.refreshTokens = refreshTokens;
        this.passwordEncoder = passwordEncoder;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional
    public UserAccount create(UUID tenantId, UUID actorId, String email, String displayName, String password,
                              Set<String> roleCodes, RequestMetadata meta) {
        Instant now = clock.instant();
        if (users.existsByTenantIdAndEmailNormalized(tenantId, UserAccount.normalizeEmail(email))) {
            throw new ApiException(IdentityErrorCode.EMAIL_TAKEN, "An account with this email already exists");
        }
        List<Role> resolved = resolveRoles(tenantId, roleCodes);
        UserAccount user = users.save(UserAccount.create(tenantId, email, passwordEncoder.encode(password),
                displayName, resolved, now));
        audit.audit(AuthAuditEntry.Type.ACCOUNT_CREATED, tenantId, user.getId(), meta,
                Map.of("createdBy", actorId.toString(), "roles", List.copyOf(user.roleCodes())), now);
        audit.publish(tenantId, "User", user.getId(), "UserRegistered",
                Map.of("userId", user.getId().toString(), "email", user.getEmail(),
                        "displayName", user.getDisplayName(), "roles", List.copyOf(user.roleCodes())), now);
        return user;
    }

    @Transactional(readOnly = true)
    public Page<UserAccount> list(UUID tenantId, Pageable pageable) {
        return users.findByTenantId(tenantId, pageable);
    }

    @Transactional(readOnly = true)
    public UserAccount get(UUID tenantId, UUID userId) {
        return users.findByIdAndTenantId(userId, tenantId).orElseThrow(() -> ApiException.notFound("Account"));
    }

    /** Disabling an account immediately revokes all of its refresh tokens. */
    @Transactional
    public UserAccount changeStatus(UUID tenantId, UUID actorId, UUID userId, UserAccount.Status status,
                                    RequestMetadata meta) {
        if (actorId.equals(userId)) {
            throw new ApiException(IdentityErrorCode.CANNOT_CHANGE_OWN_STATUS, "Ask another administrator");
        }
        Instant now = clock.instant();
        UserAccount user = get(tenantId, userId);
        user.changeStatus(status, now);
        if (status == UserAccount.Status.DISABLED) {
            refreshTokens.revokeAllForUser(userId, RefreshToken.RevokeReason.ACCOUNT_DISABLED, now);
        }
        audit.audit(AuthAuditEntry.Type.ACCOUNT_STATUS_CHANGED, tenantId, userId, meta,
                Map.of("status", status.name(), "changedBy", actorId.toString()), now);
        audit.publish(tenantId, "User", userId, "UserStatusChanged",
                Map.of("userId", userId.toString(), "status", status.name()), now);
        return user;
    }

    @Transactional(readOnly = true)
    public List<Role> rolesFor(UUID tenantId) {
        return roles.findVisibleToTenant(tenantId);
    }

    private List<Role> resolveRoles(UUID tenantId, Set<String> requested) {
        Set<String> codes = requested == null || requested.isEmpty() ? Set.of("EMPLOYEE") : new HashSet<>(requested);
        List<Role> found = roles.findVisibleByCodes(tenantId, codes);
        if (found.size() != codes.size()) {
            Set<String> missing = new TreeSet<>(codes);
            found.forEach(r -> missing.remove(r.getCode()));
            throw new ApiException(IdentityErrorCode.UNKNOWN_ROLE, "Unknown role(s): " + String.join(", ", missing));
        }
        return found;
    }
}
