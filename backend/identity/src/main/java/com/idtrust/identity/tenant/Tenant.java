package com.idtrust.identity.tenant;

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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A tenant is an isolated slice of an {@link com.idtrust.identity.organization.Organization}.
 * Corresponds to FR-ORG-004 (Create Tenant), FR-ORG-005 (Update Tenant), FR-ORG-006 (Suspend
 * Tenant) and FR-ORG-014 (Configure Tenant Settings).
 */
@Entity
@Table(name = "tenants")
public class Tenant {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TenantStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private Map<String, Object> configuration;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "lock_version", nullable = false)
    private long lockVersion;

    protected Tenant() {
        // JPA
    }

    public Tenant(UUID organizationId, String name) {
        this.id = UUID.randomUUID();
        this.organizationId = organizationId;
        this.name = name;
        this.status = TenantStatus.ACTIVE;
        this.configuration = new HashMap<>();
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void rename(String name) {
        if (name != null && !name.isBlank()) {
            this.name = name;
            this.updatedAt = Instant.now();
        }
    }

    public void updateConfiguration(Map<String, Object> configuration) {
        this.configuration = configuration == null ? new HashMap<>() : new HashMap<>(configuration);
        this.updatedAt = Instant.now();
    }

    public void suspend() {
        this.status = TenantStatus.SUSPENDED;
        this.updatedAt = Instant.now();
    }

    public void reactivate() {
        this.status = TenantStatus.ACTIVE;
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public String getName() {
        return name;
    }

    public TenantStatus getStatus() {
        return status;
    }

    public Map<String, Object> getConfiguration() {
        return configuration;
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
