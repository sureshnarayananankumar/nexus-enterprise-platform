package com.nexus.identity.infrastructure.persistence;

import com.nexus.identity.domain.AuthAuditEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuthAuditRepository extends JpaRepository<AuthAuditEntry, Long> {
    List<AuthAuditEntry> findByUserIdOrderByOccurredAtDesc(UUID userId);
}
