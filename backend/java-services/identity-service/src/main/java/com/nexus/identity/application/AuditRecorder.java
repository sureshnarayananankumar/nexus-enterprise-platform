package com.nexus.identity.application;

import com.nexus.common.error.MdcKeys;
import com.nexus.identity.domain.AuthAuditEntry;
import com.nexus.identity.domain.OutboxEvent;
import com.nexus.identity.domain.RequestMetadata;
import com.nexus.identity.infrastructure.persistence.AuthAuditRepository;
import com.nexus.identity.infrastructure.persistence.OutboxEventRepository;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** Writes auth audit rows and outbox events inside the caller's transaction. */
@Component
public class AuditRecorder {
    private final AuthAuditRepository audit;
    private final OutboxEventRepository outbox;

    public AuditRecorder(AuthAuditRepository audit, OutboxEventRepository outbox) {
        this.audit = audit;
        this.outbox = outbox;
    }

    public void audit(AuthAuditEntry.Type type, UUID tenantId, UUID userId, RequestMetadata meta,
                      Map<String, Object> details, Instant now) {
        audit.save(AuthAuditEntry.of(type, tenantId, userId, meta, details, now));
    }

    public void publish(UUID tenantId, String aggregateType, UUID aggregateId, String eventType,
                        Map<String, Object> payload, Instant now) {
        outbox.save(OutboxEvent.of(tenantId, aggregateType, aggregateId, eventType, payload,
                MDC.get(MdcKeys.CORRELATION_ID), now));
    }
}
