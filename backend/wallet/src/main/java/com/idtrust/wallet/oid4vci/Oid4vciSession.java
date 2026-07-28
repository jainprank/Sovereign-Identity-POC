package com.idtrust.wallet.oid4vci;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Tracks one credential offer through the OID4VCI pre-authorized-code grant (FR-OID4VCI-006/009/
 * 011/014), from {@link Oid4vciSessionService#createOffer} through
 * {@link Oid4vciSessionService#issueToken} and {@link Oid4vciSessionService#redeemCredential}.
 * Supersedes {@code credential.issuance.CredentialOfferGenerator}'s E07 placeholder (issuer +
 * credential id only, no claim mechanism) as the real, claimable offer - see that class's Javadoc.
 *
 * <p>The referenced {@link com.idtrust.credential.issuance.Credential} is already signed at
 * issuance time (E07); this session exists purely to gate <em>handing the existing signed JWT to a
 * caller</em> behind a one-time-use code and short-lived access token, not to trigger any new
 * signing operation.
 */
@Entity
@Table(name = "oid4vci_sessions")
public class Oid4vciSession {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "credential_id", nullable = false)
    private UUID credentialId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Oid4vciSessionStatus status;

    @Column(name = "pre_authorized_code", nullable = false)
    private String preAuthorizedCode;

    @Column(name = "access_token")
    private String accessToken;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "claimed_at")
    private Instant claimedAt;

    protected Oid4vciSession() {
        // JPA
    }

    public Oid4vciSession(UUID tenantId, UUID credentialId, String preAuthorizedCode, Instant expiresAt) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.credentialId = credentialId;
        this.status = Oid4vciSessionStatus.OFFERED;
        this.preAuthorizedCode = preAuthorizedCode;
        this.expiresAt = expiresAt;
        this.createdAt = Instant.now();
    }

    public void issueToken(String accessToken) {
        this.accessToken = accessToken;
        this.status = Oid4vciSessionStatus.TOKEN_ISSUED;
    }

    public void claim() {
        this.status = Oid4vciSessionStatus.CLAIMED;
        this.claimedAt = Instant.now();
    }

    public void expire() {
        this.status = Oid4vciSessionStatus.EXPIRED;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getCredentialId() {
        return credentialId;
    }

    public Oid4vciSessionStatus getStatus() {
        return status;
    }

    public String getPreAuthorizedCode() {
        return preAuthorizedCode;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getClaimedAt() {
        return claimedAt;
    }

    public boolean isExpired() {
        return expiresAt.isBefore(Instant.now());
    }
}
