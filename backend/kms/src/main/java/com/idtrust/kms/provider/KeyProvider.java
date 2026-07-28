package com.idtrust.kms.provider;

import com.idtrust.kms.crypto.KeyAlgorithm;
import com.idtrust.kms.key.ManagedKey;

/**
 * Backend abstraction for key generation, signing and lifecycle operations (FR-KMS-* - "provider
 * abstraction: internal encrypted store first, then AWS KMS / Vault / HSM"). The spec's
 * architecture diagram names this abstraction but does not define its method signatures; this
 * shape was designed to cover every KMS lifecycle operation the spec describes (generate, sign,
 * deactivate, delete) plus a health check, since a provider-agnostic KMS layer without a signing
 * capability would be incomplete for the credential-issuance epics (E07+) that depend on it.
 *
 * <p>Signing is intentionally not exposed via a public REST endpoint - no FR requires it, and
 * private key material/signing is meant to be used in-process by later modules (e.g. E07
 * credential issuance), never handed to API callers directly.
 */
public interface KeyProvider {

    KeyProviderType type();

    GeneratedKeyMaterial generateKey(KeyAlgorithm algorithm);

    byte[] sign(ManagedKey key, byte[] payload);

    void deactivate(ManagedKey key);

    void delete(ManagedKey key);

    boolean healthCheck();
}
