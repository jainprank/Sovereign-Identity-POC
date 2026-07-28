package com.idtrust.identity.rbac;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * A role grants a set of {@link Permission}s. {@code tenantId == null} denotes a platform-wide
 * system role (seeded via migration, e.g. PLATFORM_ADMIN, ORG_ADMIN); a non-null {@code tenantId}
 * denotes a tenant's own custom role. Corresponds to FR-ORG-011 (Create Role) and FR-ORG-013
 * (Custom Permission Sets).
 */
@Entity
@Table(name = "roles")
public class Role {

    @Id
    private UUID id;

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(name = "is_system", nullable = false)
    private boolean system;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "role_permissions",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id"))
    private Set<Permission> permissions = new HashSet<>();

    protected Role() {
        // JPA
    }

    /** Creates a tenant-owned custom role (system roles are seeded, not created via this constructor). */
    public Role(UUID tenantId, String code, String name, String description) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.code = code;
        this.name = name;
        this.description = description;
        this.system = false;
        this.createdAt = Instant.now();
    }

    public void grant(Permission permission) {
        this.permissions.add(permission);
    }

    public void revoke(Permission permission) {
        this.permissions.remove(permission);
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public boolean isSystem() {
        return system;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Set<Permission> getPermissions() {
        return permissions;
    }
}
