package com.idtrust.verification;

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
 * A verifier's OID4VP request against a holder's wallet (EPIC 10/11 - FR-VER-001 through 003,
 * FR-OID4VP-001 through 003). {@code nonce} defends against replay (Baseline section 21 threat
 * model), and is checked against the wallet's signed VP JWT's own {@code nonce} claim in
 * {@code VerificationService#presentAndVerify}. {@code presentationDefinition} (FR-OID4VP-002 -
 * DCQL/Presentation Definition) is a plain JSONB map derived from the referenced
 * {@code policy.VerificationPolicy} rather than a typed record - it exists purely to be echoed
 * back into the OID4VP request URI/QR code for the wallet to read, this platform never needs to
 * parse its own copy back out.
 *
 * <p>{@code checks} (FR-VER-013 - Verification Result Details) records the individual pass/fail
 * outcome of every check {@code presentAndVerify} ran (signature, subject binding, nonce,
 * expiry, status, trust, required claims), so a caller can see exactly why a request failed
 * rather than just a boolean.
 */
@Entity
@Table(name = "verification_requests")
public class VerificationRequest {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "policy_id", nullable = false)
    private UUID policyId;

    @Column(name = "callback_url")
    private String callbackUrl;

    @Column(nullable = false)
    private String nonce;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VerificationStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "presentation_definition", nullable = false)
    private Map<String, Object> presentationDefinition;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "checks")
    private Map<String, Object> checks;

    @Column(name = "status_reason")
    private String statusReason;

    @Column(name = "verified_credential_id")
    private UUID verifiedCredentialId;

    @Column(name = "verified_subject_did")
    private String verifiedSubjectDid;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    protected VerificationRequest() {
        // JPA
    }

    public VerificationRequest(UUID tenantId, UUID policyId, String callbackUrl, String nonce,
                                Map<String, Object> presentationDefinition, Instant expiresAt) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.policyId = policyId;
        this.callbackUrl = callbackUrl;
        this.nonce = nonce;
        this.presentationDefinition = presentationDefinition;
        this.status = VerificationStatus.PENDING;
        this.createdAt = Instant.now();
        this.expiresAt = expiresAt;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public void expire() {
        this.status = VerificationStatus.EXPIRED;
    }

    public void verify(UUID credentialId, String subjectDid, Map<String, Object> checks) {
        this.status = VerificationStatus.VERIFIED;
        this.verifiedCredentialId = credentialId;
        this.verifiedSubjectDid = subjectDid;
        this.checks = checks;
        this.verifiedAt = Instant.now();
    }

    public void fail(String reason, Map<String, Object> checks) {
        this.status = VerificationStatus.FAILED;
        this.statusReason = reason;
        this.checks = checks;
        this.verifiedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getPolicyId() {
        return policyId;
    }

    public String getCallbackUrl() {
        return callbackUrl;
    }

    public String getNonce() {
        return nonce;
    }

    public VerificationStatus getStatus() {
        return status;
    }

    public Map<String, Object> getPresentationDefinition() {
        return presentationDefinition;
    }

    public Map<String, Object> getChecks() {
        return checks;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public UUID getVerifiedCredentialId() {
        return verifiedCredentialId;
    }

    public String getVerifiedSubjectDid() {
        return verifiedSubjectDid;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getVerifiedAt() {
        return verifiedAt;
    }
}
