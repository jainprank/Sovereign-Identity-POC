package com.idtrust.kms.key;

import com.idtrust.common.audit.AuditEvent;
import com.idtrust.common.audit.AuditEventPublisher;
import com.idtrust.common.tenant.CurrentUserContext;
import com.idtrust.common.web.ApiErrorCode;
import com.idtrust.common.web.ApiException;
import com.idtrust.kms.config.KmsProperties;
import com.idtrust.kms.crypto.KeyAlgorithm;
import com.idtrust.kms.provider.GeneratedKeyMaterial;
import com.idtrust.kms.provider.KeyProvider;
import com.idtrust.kms.provider.KeyProviderRegistry;
import com.idtrust.kms.provider.KeyProviderType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;
import java.util.Map;
import java.util.UUID;

/**
 * Implements FR-KMS-* key generation, signing, rotation and deactivation. Controller-reachable
 * operations take an explicit {@code tenantId} (resolved once from {@code TenantContext} by the
 * caller, per this codebase's established convention - see {@code identity.user.UserService}),
 * scoping every lookup in addition to the Postgres RLS policies on the {@code keys} table.
 *
 * <p>{@link #linkToDid} and {@link #getActiveKeyForDid} are the exception: they are only called
 * internally by {@code com.idtrust.did.provider.DidMethodProvider} implementations, which have
 * already verified tenant ownership of the owning DID at their own layer before calling in.
 */
@Service
public class KeyService {

    private final ManagedKeyRepository keyRepository;
    private final KeyProviderRegistry providerRegistry;
    private final KmsProperties kmsProperties;
    private final AuditEventPublisher auditEventPublisher;

    public KeyService(ManagedKeyRepository keyRepository, KeyProviderRegistry providerRegistry,
                       KmsProperties kmsProperties, AuditEventPublisher auditEventPublisher) {
        this.keyRepository = keyRepository;
        this.providerRegistry = providerRegistry;
        this.kmsProperties = kmsProperties;
        this.auditEventPublisher = auditEventPublisher;
    }

    @Transactional
    public ManagedKey generate(UUID tenantId, KeyType keyType, KeyAlgorithm algorithm, KeyProviderType providerType) {
        KeyProviderType resolvedType = providerType != null ? providerType : defaultProviderType();
        KeyProvider provider = providerRegistry.get(resolvedType);
        GeneratedKeyMaterial material = provider.generateKey(algorithm);

        ManagedKey key = new ManagedKey(tenantId, keyType, algorithm, resolvedType,
                Base64.getEncoder().encodeToString(material.publicKey()),
                material.encryptedPrivateKey(), material.externalKeyId());
        keyRepository.save(key);
        audit(tenantId, "KEY_GENERATED", key.getId());
        return key;
    }

    public ManagedKey get(UUID tenantId, UUID id) {
        return keyRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new ApiException(ApiErrorCode.RESOURCE_NOT_FOUND, "Key not found: " + id));
    }

    /**
     * Links a newly generated key to the DID it backs. Called by
     * {@code com.idtrust.did.provider.DidMethodProvider} implementations after
     * {@link #generate}, once the owning DID's id is known - the key must be generated (and its
     * id returned) before the did module can construct its DID Document, so linking happens as a
     * separate step rather than as part of {@link #generate} itself.
     */
    @Transactional
    public ManagedKey linkToDid(UUID keyId, UUID didId) {
        ManagedKey key = getById(keyId);
        key.linkToDid(didId);
        return key;
    }

    /** The single key currently backing a DID's verification method, used by did:web rotation. */
    public ManagedKey getActiveKeyForDid(UUID didId) {
        return keyRepository.findByDidId(didId).stream()
                .filter(key -> key.getStatus() == KeyStatus.ACTIVE)
                .findFirst()
                .orElseThrow(() -> new ApiException(ApiErrorCode.RESOURCE_NOT_FOUND,
                        "No active key found for DID: " + didId));
    }

    public byte[] sign(UUID tenantId, UUID id, byte[] payload) {
        ManagedKey key = get(tenantId, id);
        if (key.getStatus() != KeyStatus.ACTIVE) {
            throw new ApiException(ApiErrorCode.INVALID_REQUEST, "Key is not active: " + id);
        }
        return providerRegistry.get(key.getProvider()).sign(key, payload);
    }

    @Transactional
    public ManagedKey rotate(UUID id) {
        ManagedKey previous = getById(id);
        return rotate(previous);
    }

    @Transactional
    public ManagedKey rotate(UUID tenantId, UUID id) {
        return rotate(get(tenantId, id));
    }

    @Transactional
    public ManagedKey deactivate(UUID tenantId, UUID id) {
        ManagedKey key = get(tenantId, id);
        providerRegistry.get(key.getProvider()).deactivate(key);
        key.deactivate();
        audit(tenantId, "KEY_DEACTIVATED", key.getId());
        return key;
    }

    @Transactional
    public void delete(UUID tenantId, UUID id) {
        ManagedKey key = get(tenantId, id);
        if (key.getStatus() == KeyStatus.ACTIVE) {
            throw new ApiException(ApiErrorCode.INVALID_REQUEST, "Deactivate the key before deleting it: " + id);
        }
        providerRegistry.get(key.getProvider()).delete(key);
        keyRepository.delete(key);
        audit(tenantId, "KEY_DELETED", id);
    }

    private ManagedKey rotate(ManagedKey previous) {
        if (previous.getStatus() != KeyStatus.ACTIVE) {
            throw new ApiException(ApiErrorCode.INVALID_REQUEST, "Only an active key can be rotated: " + previous.getId());
        }

        KeyProvider provider = providerRegistry.get(previous.getProvider());
        GeneratedKeyMaterial material = provider.generateKey(previous.getAlgorithm());

        ManagedKey next = new ManagedKey(previous.getTenantId(), previous.getKeyType(), previous.getAlgorithm(),
                previous.getProvider(), Base64.getEncoder().encodeToString(material.publicKey()),
                material.encryptedPrivateKey(), material.externalKeyId(), previous.getId());
        next.linkToDid(previous.getDidId());
        keyRepository.save(next);

        previous.markRotated();
        audit(previous.getTenantId(), "KEY_ROTATED", next.getId());
        return next;
    }

    private ManagedKey getById(UUID id) {
        return keyRepository.findById(id)
                .orElseThrow(() -> new ApiException(ApiErrorCode.RESOURCE_NOT_FOUND, "Key not found: " + id));
    }

    private KeyProviderType defaultProviderType() {
        String configured = kmsProperties.provider();
        if (configured == null || configured.isBlank()) {
            return KeyProviderType.INTERNAL;
        }
        return KeyProviderType.valueOf(configured.trim().toUpperCase());
    }

    private void audit(UUID tenantId, String action, UUID keyId) {
        auditEventPublisher.publish(AuditEvent.of(
                tenantId,
                CurrentUserContext.getUserId(),
                action,
                "KEY",
                keyId.toString(),
                "SUCCESS",
                Map.of()
        ));
    }
}
