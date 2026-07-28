package com.idtrust.trust;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * A tenant-owned trust registry (EPIC 13 - Trust Registry), FR-TRUST-001. Per the Baseline's
 * section 16 model, a registry is a named collection of {@link TrustedIssuer}s under an optional
 * {@code framework} label (e.g. "eIDAS", "Internal Partner Network") - a tenant may run more than
 * one (the section 7 ERD's {@code TENANT --< TRUST_REGISTRY} is one-to-many), for example to keep
 * an internal partner network separate from a regulated external framework.
 *
 * <p>{@code framework} is a plain free-text field, not a dedicated typed entity - FR-TRUST-008
 * (Trust Framework, a full hierarchy of framework metadata/governance rules) is V1-priority and out
 * of scope for this pass; this field is a documented stub for it, matching E06's precedent of
 * skipping FR-SCH-012/013.
 */
@Entity
@Table(name = "trust_registries")
public class TrustRegistry {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private String name;

    @Column
    private String framework;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RegistryStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected TrustRegistry() {
        // JPA
    }

    public TrustRegistry(UUID tenantId, String name, String framework) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.name = name;
        this.framework = framework;
        this.status = RegistryStatus.ACTIVE;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void deactivate() {
        this.status = RegistryStatus.INACTIVE;
        this.updatedAt = Instant.now();
    }

    public void activate() {
        this.status = RegistryStatus.ACTIVE;
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

    public String getFramework() {
        return framework;
    }

    public RegistryStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
