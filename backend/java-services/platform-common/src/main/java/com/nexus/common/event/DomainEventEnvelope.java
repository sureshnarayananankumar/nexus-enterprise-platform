package com.nexus.common.event;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Versioned event contract shared by all services (see docs/architecture/03-api-communication.md §4).
 */
public record DomainEventEnvelope(
        UUID eventId,
        String eventType,
        int eventVersion,
        Instant occurredAt,
        UUID tenantId,
        String aggregateType,
        String aggregateId,
        String correlationId,
        Actor actor,
        Map<String, Object> payload) {

    public record Actor(String type, String id) {
        public static Actor user(UUID userId) { return new Actor("USER", userId == null ? null : userId.toString()); }
        public static Actor system() { return new Actor("SYSTEM", null); }
    }
}
