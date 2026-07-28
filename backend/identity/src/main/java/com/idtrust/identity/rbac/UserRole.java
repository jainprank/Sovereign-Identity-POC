package com.idtrust.identity.rbac;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * A single role assignment: grants {@code roleId} to {@code userId} within {@code tenantId}.
 * Corresponds to FR-ORG-012 (Assign Role).
 */
@Entity
@Table(name = "user_roles")
public class UserRole {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "role_id", nullable = false)
    private UUID roleId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt;

    protected UserRole() {
        // JPA
    }

    public UserRole(UUID userId, UUID roleId, UUID tenantId) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.roleId = roleId;
        this.tenantId = tenantId;
        this.assignedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getRoleId() {
        return roleId;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public Instant getAssignedAt() {
        return assignedAt;
    }
}
