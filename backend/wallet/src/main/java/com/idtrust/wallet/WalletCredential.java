package com.idtrust.wallet;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * A credential a {@link Wallet} has received and stored (FR-WAL-004 - Receive Credential,
 * FR-WAL-005 - Store Credential). Deliberately holds only a reference to the {@code credentials}
 * row (id) rather than a copy of {@code credential_jwt}: the underlying {@code Credential} (issued
 * and signed in {@code credential.issuance}) is this platform's single source of truth for the VC
 * itself and its lifecycle status - duplicating the JWT here would just be another place for it to
 * drift out of sync with a subsequent revoke/suspend/renew (EPIC 7).
 *
 * <p>{@code category} (FR-WAL-009 - Credential Categories) is free-text and optional; no fixed
 * category taxonomy is specified anywhere in the Baseline.
 *
 * <p>Deleting a wallet's copy (FR-WAL-007) removes this row only - it never touches the underlying
 * {@code Credential} or its status, which remains the issuer's record regardless of what any
 * holder does with their own copy.
 */
@Entity
@Table(name = "wallet_credentials")
public class WalletCredential {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "wallet_id", nullable = false)
    private UUID walletId;

    @Column(name = "credential_id", nullable = false)
    private UUID credentialId;

    @Column
    private String category;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    protected WalletCredential() {
        // JPA
    }

    public WalletCredential(UUID tenantId, UUID walletId, UUID credentialId, String category) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.walletId = walletId;
        this.credentialId = credentialId;
        this.category = category;
        this.receivedAt = Instant.now();
    }

    public void recategorize(String category) {
        this.category = category;
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

    public UUID getCredentialId() {
        return credentialId;
    }

    public String getCategory() {
        return category;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }
}
