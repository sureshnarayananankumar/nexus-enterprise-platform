package com.nexus.identity.domain;

import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** A bundle of permissions. tenantId == null means a platform system role. */
@Entity
@Table(name = "roles")
public class Role {

    @Id
    private UUID id;

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 255)
    private String description;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "role_permissions", joinColumns = @JoinColumn(name = "role_id"))
    @Column(name = "permission_code", length = 100)
    private Set<String> permissions = new HashSet<>();

    protected Role() {}

    public boolean isSystem() { return tenantId == null; }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public Set<String> getPermissions() { return Set.copyOf(permissions); }
}
