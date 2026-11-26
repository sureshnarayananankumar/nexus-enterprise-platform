package com.nexus.identity.infrastructure.persistence;

import com.nexus.identity.domain.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<Role, UUID> {

    /** System roles plus the tenant's own custom roles. */
    @Query("select r from Role r where r.tenantId is null or r.tenantId = :tenantId order by r.code")
    List<Role> findVisibleToTenant(@Param("tenantId") UUID tenantId);

    @Query("select r from Role r where (r.tenantId is null or r.tenantId = :tenantId) and r.code in :codes")
    List<Role> findVisibleByCodes(@Param("tenantId") UUID tenantId, @Param("codes") Collection<String> codes);

    @Query("select r from Role r where r.tenantId is null and r.code = :code")
    Optional<Role> findSystemRole(@Param("code") String code);
}
