package com.idtrust.identity.serviceaccount;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * A tenant-owned, non-human identity (e.g. an internal automation/system actor) that activity can
 * be attributed to. Distinct from {@code apiplatform.client.ApiClient}, which models the
 * *credential* a machine caller authenticates with (API key / OAuth client) - a service account is
 * the durable identity behind one or more such credentials, and outlives any individual
 * credential's rotation or revocation.
 */
@Entity
@Table(name = "service_accounts")
public class ServiceAccount {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private String name;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ServiceAccountStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ServiceAccount() {
        // JPA
    }

    public ServiceAccount(UUID tenantId, String name, String description) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.name = name;
        this.description = description;
        this.status = ServiceAccountStatus.ACTIVE;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void suspend() {
        this.status = ServiceAccountStatus.SUSPENDED;
        this.updatedAt = Instant.now();
    }

    public void reactivate() {
        this.status = ServiceAccountStatus.ACTIVE;
        this.updatedAt = Instant.now();
    }

    public void revoke() {
        this.status = ServiceAccountStatus.REVOKED;
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

    public String getDescription() {
        return description;
    }

    public ServiceAccountStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
