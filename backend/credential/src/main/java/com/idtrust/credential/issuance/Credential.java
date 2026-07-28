package com.idtrust.credential.issuance;

import com.idtrust.credential.schema.CredentialFormat;
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
 * An issued verifiable credential (FR-ISS-012). No separate "credential definition" entity exists
 * (FR-ISS-001/002 are folded into the issuance request itself - see
 * {@code CredentialIssuanceService#issue}), matching the Baseline's literal {@code credentials}
 * DDL, which has no such table.
 *
 * <p>Like {@code com.idtrust.did.Did}, the id is supplied to the constructor rather than generated
 * inside it: the JWT-VC's {@code jti} claim embeds the credential id, so the id must be known
 * before the signed JWT (and therefore this entity) can be constructed.
 *
 * <p>{@code claims} (added in E08, {@code V10__credential_status_lifecycle.sql}) is a documented
 * extension beyond the Baseline's literal DDL, which only stores {@code credential_hash}: EPIC 7's
 * renewal (FR-CRED-009) and reissuance (FR-CRED-010) both need to re-sign a new credential from
 * the original's claims, and decoding them back out of {@code credential_jwt} would be a fragile
 * detour around data the platform already had at issuance time.
 *
 * <p>Status transition methods ({@link #revoke}, {@link #suspend}, {@link #reinstate},
 * {@link #expire}) are intentionally "dumb" - they perform no validation of the current state.
 * Guarding which transitions are legal (e.g. only an {@code ISSUED} credential can be suspended)
 * is the service layer's job, matching the {@code requireDraft}/{@code requirePublished} pattern
 * already established in {@code CredentialSchemaService}.
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

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private Map<String, Object> claims;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CredentialStatus status;

    @Column(name = "status_reason")
    private String statusReason;

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
                       CredentialFormat format, String credentialJwt, String credentialHash,
                       Map<String, Object> claims, Instant expiresAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.schemaId = schemaId;
        this.issuerDid = issuerDid;
        this.subjectDid = subjectDid;
        this.format = format;
        this.credentialJwt = credentialJwt;
        this.credentialHash = credentialHash;
        this.claims = claims;
        this.status = CredentialStatus.ISSUED;
        this.expiresAt = expiresAt;
        Instant now = Instant.now();
        this.issuedAt = now;
        this.createdAt = now;
        this.updatedAt = now;
    }

    /** FR-CRED-005 - permanent; a revoked credential can never return to any other status. */
    public void revoke(String reason) {
        this.status = CredentialStatus.REVOKED;
        this.statusReason = reason;
        this.updatedAt = Instant.now();
    }

    /** FR-CRED-006 - temporary; see {@link #reinstate()}. */
    public void suspend(String reason) {
        this.status = CredentialStatus.SUSPENDED;
        this.statusReason = reason;
        this.updatedAt = Instant.now();
    }

    /** FR-CRED-007 - reverses {@link #suspend}. */
    public void reinstate() {
        this.status = CredentialStatus.ISSUED;
        this.statusReason = null;
        this.updatedAt = Instant.now();
    }

    /** FR-CRED-008 - applied lazily on read rather than by a background sweep; see CredentialLifecycleService. */
    public void expire(String reason) {
        this.status = CredentialStatus.EXPIRED;
        this.statusReason = reason;
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

    public Map<String, Object> getClaims() {
        return claims;
    }

    public String getStatusReason() {
        return statusReason;
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
