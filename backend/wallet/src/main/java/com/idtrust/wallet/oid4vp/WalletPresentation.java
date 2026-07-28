package com.idtrust.wallet.oid4vp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * A record of a wallet having generated and signed a Verifiable Presentation (FR-WAL-017 -
 * Presentation History). Holds the full {@code presentationJwt} (unlike {@code
 * wallet.WalletCredential}, which deliberately holds only a reference to its {@code Credential} -
 * a VP JWT is a one-time, nonce-bound artifact rather than a durable resource with its own
 * lifecycle, so there's no drift risk in storing it directly).
 *
 * <p>{@code audience}/{@code nonce} are recorded exactly as presented, purely for audit/history
 * purposes - the wallet module has no dependency on {@code verification} (see this module's
 * README section for that deliberate module-boundary decision) and never checks them itself; only
 * the verifier-side {@code VerificationService#presentAndVerify} does.
 */
@Entity
@Table(name = "wallet_presentations")
public class WalletPresentation {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "wallet_id", nullable = false)
    private UUID walletId;

    @Column(name = "wallet_credential_id", nullable = false)
    private UUID walletCredentialId;

    @Column(nullable = false)
    private String audience;

    @Column(nullable = false)
    private String nonce;

    @Column(name = "presentation_jwt", nullable = false)
    private String presentationJwt;

    @Column(name = "presented_at", nullable = false)
    private Instant presentedAt;

    protected WalletPresentation() {
        // JPA
    }

    public WalletPresentation(UUID tenantId, UUID walletId, UUID walletCredentialId, String audience, String nonce,
                               String presentationJwt) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.walletId = walletId;
        this.walletCredentialId = walletCredentialId;
        this.audience = audience;
        this.nonce = nonce;
        this.presentationJwt = presentationJwt;
        this.presentedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getWalletId() {
        return walletId;
    }

    public UUID getWalletCredentialId() {
        return walletCredentialId;
    }

    public String getAudience() {
        return audience;
    }

    public String getNonce() {
        return nonce;
    }

    public String getPresentationJwt() {
        return presentationJwt;
    }

    public Instant getPresentedAt() {
        return presentedAt;
    }
}
