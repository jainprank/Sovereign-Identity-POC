package com.idtrust.credential.template;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * A reusable, tenant-owned issuance preset for a {@code credential_schemas} entry: a default claim
 * payload and default validity period, so an issuer needn't resupply the same boilerplate claims
 * on every issuance request. Distinct from {@code CredentialSchema} (which defines the claim
 * catalogue a credential must validate against) - see {@code V35__credential_templates_core.sql}.
 */
@Entity
@Table(name = "credential_templates")
public class CredentialTemplate {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "schema_id", nullable = false)
    private UUID schemaId;

    @Column(nullable = false)
    private String name;

    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "default_claims", nullable = false)
    private Map<String, Object> defaultClaims;

    @Column(name = "default_expires_in_days")
    private Integer defaultExpiresInDays;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CredentialTemplate() {
        // JPA
    }

    public CredentialTemplate(UUID tenantId, UUID schemaId, String name, String description,
                               Map<String, Object> defaultClaims, Integer defaultExpiresInDays) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.schemaId = schemaId;
        this.name = name;
        this.description = description;
        this.defaultClaims = defaultClaims != null ? defaultClaims : Map.of();
        this.defaultExpiresInDays = defaultExpiresInDays;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void update(String description, Map<String, Object> defaultClaims, Integer defaultExpiresInDays) {
        this.description = description;
        this.defaultClaims = defaultClaims != null ? defaultClaims : Map.of();
        this.defaultExpiresInDays = defaultExpiresInDays;
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getSchemaId() {
        return schemaId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Map<String, Object> getDefaultClaims() {
        return defaultClaims;
    }

    public Integer getDefaultExpiresInDays() {
        return defaultExpiresInDays;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
