package com.idtrust.did;

import com.idtrust.did.provider.DidMethod;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * A DID managed by this platform (FR-DID-*). Unlike other entities in this codebase, the id is
 * supplied to the constructor rather than generated inside it: {@code DidService} must know the
 * id before the DID string/document exist, so it can pass the id to
 * {@link com.idtrust.did.provider.DidMethodProvider#create} for the generated signing key to be
 * linked back to (see {@code com.idtrust.kms.key.KeyService#linkToDid}); only once the provider
 * returns does the final {@code did}/{@code document} become known.
 */
@Entity
@Table(name = "dids")
public class Did {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private String did;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DidMethod method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DidStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private Map<String, Object> document;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Did() {
        // JPA
    }

    public Did(UUID id, UUID tenantId, DidMethod method, String did, Map<String, Object> document) {
        this.id = id;
        this.tenantId = tenantId;
        this.method = method;
        this.did = did;
        this.document = document;
        this.status = DidStatus.ACTIVE;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void replaceDocument(Map<String, Object> document) {
        this.document = document;
        this.updatedAt = Instant.now();
    }

    public void deactivate() {
        this.status = DidStatus.DEACTIVATED;
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getDid() {
        return did;
    }

    public DidMethod getMethod() {
        return method;
    }

    public DidStatus getStatus() {
        return status;
    }

    public Map<String, Object> getDocument() {
        return document;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
