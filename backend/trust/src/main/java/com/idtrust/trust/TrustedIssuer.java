package com.idtrust.trust;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * A single trusted-issuer entry within a {@link TrustRegistry} (EPIC 13). Folds FR-TRUST-002
 * (Register Issuer) and FR-TRUST-003 (Register DID) into one row rather than two linked entities -
 * the Baseline's own section 16 example JSON ({@code issuerDid}/{@code organization}/
 * {@code trustedCredentialTypes}/{@code status}) never distinguishes "the issuer" from "the DID",
 * and this platform has no notion of an external organization identity independent of its DID
 * anywhere else either. Mirrors E10's precedent of folding FR-ISS-001/002 into a single issuance
 * call.
 *
 * <p>{@code issuerDid} is a plain string, not a foreign key into {@code did.Did} - trust registries
 * exist precisely to declare trust in *external* issuers (other organizations, other tenants,
 * other platforms entirely) whose DIDs this platform never provisioned and has no row for.
 *
 * <p>{@code trustedCredentialTypes} is stored as a raw JSONB string array (not wrapped in a record
 * like {@code CredentialSchema}'s {@code SchemaDefinition}) since it is scalar data with nothing to
 * query into - a null/empty list means "trusted for every credential type", matching
 * {@code PolicyDefinition.trustedIssuerDids}'s "empty list is not a fail-closed default" precedent
 * from E10. Entries name credential types by schema name (e.g. {@code "EmployeeCredential"}), the
 * same string embedded in a credential's {@code vc.type}/{@code vct} claim - see
 * {@code verification.VerificationService#decodeCredentialArtifact}.
 */
@Entity
@Table(name = "trusted_issuers")
public class TrustedIssuer {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "trust_registry_id", nullable = false)
    private UUID trustRegistryId;

    @Column(name = "issuer_did", nullable = false)
    private String issuerDid;

    @Column
    private String organization;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "trusted_credential_types")
    private List<String> trustedCredentialTypes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TrustStatus status;

    @Column(name = "registered_at", nullable = false)
    private Instant registeredAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected TrustedIssuer() {
        // JPA
    }

    public TrustedIssuer(UUID tenantId, UUID trustRegistryId, String issuerDid, String organization,
                          List<String> trustedCredentialTypes) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.trustRegistryId = trustRegistryId;
        this.issuerDid = issuerDid;
        this.organization = organization;
        this.trustedCredentialTypes = trustedCredentialTypes;
        this.status = TrustStatus.TRUSTED;
        Instant now = Instant.now();
        this.registeredAt = now;
        this.updatedAt = now;
    }

    /** FR-TRUST-004 - reversible; only between {@code TRUSTED} and {@code SUSPENDED}, see {@code TrustRegistryService}. */
    public void updateStatus(TrustStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    /** FR-TRUST-005 - terminal; see {@link TrustStatus}'s Javadoc. */
    public void revoke() {
        this.status = TrustStatus.REVOKED;
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getTrustRegistryId() {
        return trustRegistryId;
    }

    public String getIssuerDid() {
        return issuerDid;
    }

    public String getOrganization() {
        return organization;
    }

    public List<String> getTrustedCredentialTypes() {
        return trustedCredentialTypes;
    }

    public TrustStatus getStatus() {
        return status;
    }

    public Instant getRegisteredAt() {
        return registeredAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
