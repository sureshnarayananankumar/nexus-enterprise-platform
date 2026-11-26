package com.nexus.identity.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** Transactional outbox row; relayed to Kafka topic nexus.identity.events (Phase 10). */
@Entity
@Table(name = "outbox_events")
public class OutboxEvent {

    @Id
    private UUID id;

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "aggregate_type", nullable = false, length = 60)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false, length = 64)
    private String aggregateId;

    @Column(name = "event_type", nullable = false, length = 80)
    private String eventType;

    @Column(name = "event_version", nullable = false)
    private int eventVersion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> payload;

    @Column(name = "correlation_id", length = 64)
    private String correlationId;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    protected OutboxEvent() {}

    public static OutboxEvent of(UUID tenantId, String aggregateType, UUID aggregateId, String eventType,
                                 Map<String, Object> payload, String correlationId, Instant now) {
        OutboxEvent e = new OutboxEvent();
        e.id = UUID.randomUUID();
        e.tenantId = tenantId;
        e.aggregateType = aggregateType;
        e.aggregateId = aggregateId.toString();
        e.eventType = eventType;
        e.eventVersion = 1;
        e.payload = payload;
        e.correlationId = correlationId;
        e.occurredAt = now;
        return e;
    }

    public String getEventType() { return eventType; }
    public UUID getTenantId() { return tenantId; }
    public Map<String, Object> getPayload() { return payload; }
}
