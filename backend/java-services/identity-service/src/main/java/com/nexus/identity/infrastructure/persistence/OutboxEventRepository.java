package com.nexus.identity.infrastructure.persistence;

import com.nexus.identity.domain.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
    List<OutboxEvent> findByTenantIdOrderByOccurredAt(UUID tenantId);
}
