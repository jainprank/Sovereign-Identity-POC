package com.idtrust.kms.key;

import com.idtrust.kms.crypto.KeyAlgorithm;
import com.idtrust.kms.provider.KeyProviderType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * A cryptographic key managed by the KMS module (FR-KMS-*). {@code didId} is a plain UUID column
 * rather than a JPA relationship to {@code com.idtrust.did.Did}: the {@code did} module already
 * depends on {@code kms} (for signing), so a back-reference here would create a circular Gradle
 * module dependency. Referential integrity is still enforced by the {@code keys.did_id} foreign
 * key at the database layer.
 */
@Entity
@Table(name = "keys")
public class ManagedKey {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "did_id")
    private UUID didId;

    @Enumerated(EnumType.STRING)
    @Column(name = "key_type", nullable = false)
    private KeyType keyType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private KeyAlgorithm algorithm;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private KeyProviderType provider;

    @Column(name = "external_key_id")
    private String externalKeyId;

    @Column(name = "public_key", nullable = false)
    private String publicKey;

    @Column(name = "encrypted_private_key")
    private String encryptedPrivateKey;

    @Column(name = "rotated_from_key_id")
    private UUID rotatedFromKeyId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private KeyStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ManagedKey() {
        // JPA
    }

    public ManagedKey(UUID tenantId, KeyType keyType, KeyAlgorithm algorithm, KeyProviderType provider,
                       String publicKey, String encryptedPrivateKey, String externalKeyId) {
        this(tenantId, keyType, algorithm, provider, publicKey, encryptedPrivateKey, externalKeyId, null);
    }

    public ManagedKey(UUID tenantId, KeyType keyType, KeyAlgorithm algorithm, KeyProviderType provider,
                       String publicKey, String encryptedPrivateKey, String externalKeyId,
                       UUID rotatedFromKeyId) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.keyType = keyType;
        this.algorithm = algorithm;
        this.provider = provider;
        this.publicKey = publicKey;
        this.encryptedPrivateKey = encryptedPrivateKey;
        this.externalKeyId = externalKeyId;
        this.rotatedFromKeyId = rotatedFromKeyId;
        this.status = KeyStatus.ACTIVE;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void linkToDid(UUID didId) {
        this.didId = didId;
        this.updatedAt = Instant.now();
    }

    public void markRotated() {
        this.status = KeyStatus.ROTATED;
        this.updatedAt = Instant.now();
    }

    public void deactivate() {
        this.status = KeyStatus.DEACTIVATED;
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getDidId() {
        return didId;
    }

    public KeyType getKeyType() {
        return keyType;
    }

    public KeyAlgorithm getAlgorithm() {
        return algorithm;
    }

    public KeyProviderType getProvider() {
        return provider;
    }

    public String getExternalKeyId() {
        return externalKeyId;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public String getEncryptedPrivateKey() {
        return encryptedPrivateKey;
    }

    public UUID getRotatedFromKeyId() {
        return rotatedFromKeyId;
    }

    public KeyStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
