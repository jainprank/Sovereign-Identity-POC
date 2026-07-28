package com.idtrust.kms.provider;

/**
 * Result of {@link KeyProvider#generateKey}. {@code publicKey} is always populated (X.509 DER,
 * base64-independent - callers encode as needed). Exactly one of {@code encryptedPrivateKey}
 * (internal store: private key material envelope-encrypted by
 * {@link com.idtrust.kms.crypto.KeyEncryptionService}) or {@code externalKeyId} (external
 * providers: AWS KMS/Vault/HSM keep the private key outside this application entirely) is set,
 * matching which {@link KeyProviderType} produced it.
 */
public record GeneratedKeyMaterial(byte[] publicKey, String encryptedPrivateKey, String externalKeyId) {
}
