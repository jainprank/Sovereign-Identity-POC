package com.idtrust.credential.issuance;

import com.idtrust.credential.schema.CredentialFormat;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * An issued verifiable credential (FR-ISS-012). No separate "credential definition" entity exists
 * (FR-ISS-001/002 are folded into the issuance request itself - see
 * {@code CredentialIssuanceService#issue}), matching the Baseline's literal {@code credentials}
 * DDL, which has no such table.
 *
 * <p>Like {@code com.idtrust.did.Did}, the id is supplied to the constructor rather than generated
 * inside it: the JWT-VC's {@code jti} claim embeds the credential id, so the id must be known
 * before the signed JWT (and therefore this entity) can be constructed.
 */
@Entity
@Table(name = "credentials")
public class Credential {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "schema_id", nullable = false)
    private UUID schemaId;

    @Column(name = "issuer_did", nullable = false)
    private String issuerDid;

    @Column(name = "subject_did")
    private String subjectDid;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CredentialFormat format;

    @Column(name = "credential_jwt", nullable = false)
    private String credentialJwt;

    @Column(name = "credential_hash", nullable = false)
    private String credentialHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CredentialStatus status;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Credential() {
        // JPA
    }

    public Credential(UUID id, UUID tenantId, UUID schemaId, String issuerDid, String subjectDid,
                       CredentialFormat format, String credentialJwt, String credentialHash, Instant expiresAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.schemaId = schemaId;
        this.issuerDid = issuerDid;
        this.subjectDid = subjectDid;
        this.format = format;
        this.credentialJwt = credentialJwt;
        this.credentialHash = credentialHash;
        this.status = CredentialStatus.ISSUED;
        this.expiresAt = expiresAt;
        Instant now = Instant.now();
        this.issuedAt = now;
        this.createdAt = now;
        this.updatedAt = now;
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

    public String getIssuerDid() {
        return issuerDid;
    }

    public String getSubjectDid() {
        return subjectDid;
    }

    public CredentialFormat getFormat() {
        return format;
    }

    public String getCredentialJwt() {
        return credentialJwt;
    }

    public String getCredentialHash() {
        return credentialHash;
    }

    public CredentialStatus getStatus() {
        return status;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
