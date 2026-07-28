package com.idtrust.verification.policy;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * A tenant-owned verification policy (EPIC 12 - Policy Engine). Versioned by row exactly like
 * {@code credential.schema.CredentialSchema}: activating (FR-POL-004) freezes a version so
 * verification requests can safely reference it, and versioning creates a new row with the same
 * {@code name} rather than mutating an active policy's rules out from under requests already in
 * flight against it. {@code version} is an integer here (unlike {@code CredentialSchema.version}'s
 * free-text string), matching the Baseline's literal {@code verification_policies} DDL.
 */
@Entity
@Table(name = "verification_policies")
public class VerificationPolicy {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private int version;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "policy", nullable = false)
    private PolicyDefinition definition;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PolicyStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "lock_version", nullable = false)
    private long lockVersion;

    protected VerificationPolicy() {
        // JPA
    }

    public VerificationPolicy(UUID tenantId, String name, int version, PolicyDefinition definition) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.name = name;
        this.version = version;
        this.definition = definition;
        this.status = PolicyStatus.DRAFT;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void replaceDefinition(PolicyDefinition definition) {
        if (definition != null) {
            this.definition = definition;
            this.updatedAt = Instant.now();
        }
    }

    public void activate() {
        this.status = PolicyStatus.ACTIVE;
        this.updatedAt = Instant.now();
    }

    public void deactivate() {
        this.status = PolicyStatus.INACTIVE;
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getName() {
        return name;
    }

    public int getVersion() {
        return version;
    }

    public PolicyDefinition getDefinition() {
        return definition;
    }

    public PolicyStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public long getLockVersion() {
        return lockVersion;
    }
}
