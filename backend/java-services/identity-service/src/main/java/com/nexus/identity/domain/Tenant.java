package com.nexus.identity.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tenants")
public class Tenant {

    public enum Status { ACTIVE, SUSPENDED }

    @Id
    private UUID id;

    @Column(nullable = false, length = 63, unique = true)
    private String slug;

    @Column(nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Status status = Status.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version; // null until persisted -> Spring Data treats the entity as new

    protected Tenant() {}

    public static Tenant create(String slug, String name, Instant now) {
        Tenant t = new Tenant();
        t.id = UUID.randomUUID();
        t.slug = slug;
        t.name = name;
        t.createdAt = now;
        t.updatedAt = now;
        return t;
    }

    public boolean isActive() { return status == Status.ACTIVE; }

    public UUID getId() { return id; }
    public String getSlug() { return slug; }
    public String getName() { return name; }
    public Status getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}
