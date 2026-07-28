package com.idtrust.credential.schema;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * A tenant-owned credential schema (EPIC 5 - Credential Schemas). A schema is versioned by row:
 * publishing (FR-SCH-005) freezes a version so issuance can safely reference it, and versioning
 * (FR-SCH-004) creates a new row with the same {@code name} rather than mutating an existing one -
 * see {@code CredentialSchemaService#createNewVersion}.
 */
@Entity
@Table(name = "credential_schemas")
public class CredentialSchema {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String version;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CredentialFormat format;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "schema", nullable = false)
    private SchemaDefinition definition;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SchemaStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CredentialSchema() {
        // JPA
    }

    public CredentialSchema(UUID tenantId, String name, String version, CredentialFormat format,
                             SchemaDefinition definition) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.name = name;
        this.version = version;
        this.format = format;
        this.definition = definition;
        this.status = SchemaStatus.DRAFT;
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

    public void replaceDefinition(SchemaDefinition definition) {
        if (definition != null) {
            this.definition = definition;
            this.updatedAt = Instant.now();
        }
    }

    public void publish() {
        this.status = SchemaStatus.PUBLISHED;
        this.updatedAt = Instant.now();
    }

    public void deprecate() {
        this.status = SchemaStatus.DEPRECATED;
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

    public String getVersion() {
        return version;
    }

    public CredentialFormat getFormat() {
        return format;
    }

    public SchemaDefinition getDefinition() {
        return definition;
    }

    public SchemaStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
