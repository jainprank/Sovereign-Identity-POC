package com.idtrust.wallet;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * A holder's digital wallet (EPIC 8, FR-WAL-001 - Create Wallet). {@code didId}/{@code did}
 * (FR-WAL-002 - Wallet DID) are populated by creating a real {@code did:key} DID through the
 * existing {@code did} module at wallet-creation time - {@code DidMethodProvider.create} already
 * generates and stores the wallet's signing key via {@code kms} as part of that call (FR-WAL-003 -
 * Wallet Key), so no separate key-provisioning step is needed here.
 *
 * <p>{@code ownerUserId} is a plain UUID column with no JPA relationship into the {@code identity}
 * module, mirroring the {@code keys.did_id} precedent (E04/E05): {@code wallet} depends on
 * {@code did}/{@code credential} but must not gain a dependency on {@code identity} just to
 * reference the owning user.
 */
@Entity
@Table(name = "wallets")
public class Wallet {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "owner_user_id")
    private UUID ownerUserId;

    @Column(name = "did_id", nullable = false)
    private UUID didId;

    @Column(nullable = false)
    private String did;

    @Column
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WalletStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Wallet() {
        // JPA
    }

    public Wallet(UUID tenantId, UUID ownerUserId, UUID didId, String did, String label) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.ownerUserId = ownerUserId;
        this.didId = didId;
        this.did = did;
        this.label = label;
        this.status = WalletStatus.ACTIVE;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getOwnerUserId() {
        return ownerUserId;
    }

    public UUID getDidId() {
        return didId;
    }

    public String getDid() {
        return did;
    }

    public String getLabel() {
        return label;
    }

    public WalletStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
