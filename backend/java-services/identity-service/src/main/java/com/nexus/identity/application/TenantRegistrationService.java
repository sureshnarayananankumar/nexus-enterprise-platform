package com.nexus.identity.application;

import com.nexus.common.error.ApiException;
import com.nexus.identity.domain.AuthAuditEntry;
import com.nexus.identity.domain.RequestMetadata;
import com.nexus.identity.domain.Role;
import com.nexus.identity.domain.Tenant;
import com.nexus.identity.domain.UserAccount;
import com.nexus.identity.infrastructure.persistence.RoleRepository;
import com.nexus.identity.infrastructure.persistence.TenantRepository;
import com.nexus.identity.infrastructure.persistence.UserAccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Self-service tenant sign-up: creates the tenant and its first TENANT_ADMIN account atomically. */
@Service
public class TenantRegistrationService {
    public static final String ADMIN_ROLE = "TENANT_ADMIN";

    private final TenantRepository tenants;
    private final UserAccountRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder passwordEncoder;
    private final AuditRecorder audit;
    private final Clock clock;

    public TenantRegistrationService(TenantRepository tenants, UserAccountRepository users, RoleRepository roles,
                                     PasswordEncoder passwordEncoder, AuditRecorder audit, Clock clock) {
        this.tenants = tenants;
        this.users = users;
        this.roles = roles;
        this.passwordEncoder = passwordEncoder;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional
    public Registered register(String tenantName, String slug, String adminEmail, String adminPassword,
                               String adminDisplayName, RequestMetadata meta) {
        Instant now = clock.instant();
        String normalizedSlug = slug.trim().toLowerCase();
        if (tenants.existsBySlug(normalizedSlug)) {
            throw new ApiException(IdentityErrorCode.TENANT_SLUG_TAKEN, "Tenant slug '" + normalizedSlug + "' is taken");
        }
        Role admin = roles.findSystemRole(ADMIN_ROLE)
                .orElseThrow(() -> new IllegalStateException("System role TENANT_ADMIN missing; check migrations"));

        Tenant tenant = tenants.save(Tenant.create(normalizedSlug, tenantName.trim(), now));
        UserAccount user = users.save(UserAccount.create(tenant.getId(), adminEmail,
                passwordEncoder.encode(adminPassword), adminDisplayName, List.of(admin), now));

        audit.audit(AuthAuditEntry.Type.TENANT_CREATED, tenant.getId(), user.getId(), meta,
                Map.of("slug", normalizedSlug), now);
        audit.publish(tenant.getId(), "Tenant", tenant.getId(), "TenantCreated",
                Map.of("tenantId", tenant.getId().toString(), "slug", normalizedSlug, "name", tenant.getName()), now);
        audit.publish(tenant.getId(), "User", user.getId(), "UserRegistered",
                Map.of("userId", user.getId().toString(), "email", user.getEmail(),
                        "displayName", user.getDisplayName(), "roles", List.copyOf(user.roleCodes())), now);
        return new Registered(tenant.getId(), tenant.getSlug(), user.getId());
    }

    public record Registered(UUID tenantId, String slug, UUID adminUserId) {}
}
