package com.idtrust.identity.membership;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Grants a user access to a tenant beyond their {@code users.tenant_id} "home tenant", with a
 * coarse per-tenant role. See {@code V31__tenant_memberships_core.sql} for how this relates to
 * {@code AppUser} and {@code rbac.UserRole}.
 */
@Entity
@Table(name = "tenant_memberships")
public class TenantMembership {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MembershipRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MembershipStatus status;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    protected TenantMembership() {
        // JPA
    }

    public TenantMembership(UUID tenantId, UUID userId, MembershipRole role) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.userId = userId;
        this.role = role;
        this.status = MembershipStatus.ACTIVE;
        this.joinedAt = Instant.now();
    }

    public void changeRole(MembershipRole role) {
        this.role = role;
    }

    public void revoke() {
        this.status = MembershipStatus.REVOKED;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getUserId() {
        return userId;
    }

    public MembershipRole getRole() {
        return role;
    }

    public MembershipStatus getStatus() {
        return status;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }
}
