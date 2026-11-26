package com.nexus.identity.infrastructure.persistence;

import com.nexus.identity.domain.UserAccount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/** All lookups are tenant-scoped: there is deliberately no findById exposed to application code. */
public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {
    Optional<UserAccount> findByTenantIdAndEmailNormalized(UUID tenantId, String emailNormalized);
    Optional<UserAccount> findByIdAndTenantId(UUID id, UUID tenantId);
    boolean existsByTenantIdAndEmailNormalized(UUID tenantId, String emailNormalized);
    Page<UserAccount> findByTenantId(UUID tenantId, Pageable pageable);
}
