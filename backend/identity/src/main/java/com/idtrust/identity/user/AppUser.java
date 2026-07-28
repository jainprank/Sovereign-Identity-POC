package com.idtrust.identity.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * A tenant-scoped user account. Named {@code AppUser} (rather than {@code User}) to avoid
 * clashing with the {@code users} SQL keyword and {@code java.security.acl.Owner}-style ambiguity
 * used elsewhere in the codebase. Corresponds to FR-ORG-005 (User Lifecycle), FR-ORG-007 (Invite
 * User) and FR-ORG-008 (Accept Invitation).
 */
@Entity
@Table(name = "users")
public class AppUser {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "keycloak_subject_id")
    private String keycloakSubjectId;

    @Column(nullable = false)
    private String email;

    @Column(name = "display_name")
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected AppUser() {
        // JPA
    }

    public AppUser(UUID tenantId, String email, String displayName) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.email = email;
        this.displayName = displayName;
        this.status = UserStatus.INVITED;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    /** Links the local account to a Keycloak subject and activates it (FR-ORG-008). */
    public void linkKeycloakSubject(String keycloakSubjectId) {
        this.keycloakSubjectId = keycloakSubjectId;
        this.status = UserStatus.ACTIVE;
        this.updatedAt = Instant.now();
    }

    public void suspend() {
        this.status = UserStatus.SUSPENDED;
        this.updatedAt = Instant.now();
    }

    public void reactivate() {
        this.status = UserStatus.ACTIVE;
        this.updatedAt = Instant.now();
    }

    public void deactivate() {
        this.status = UserStatus.DEACTIVATED;
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getKeycloakSubjectId() {
        return keycloakSubjectId;
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public UserStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
