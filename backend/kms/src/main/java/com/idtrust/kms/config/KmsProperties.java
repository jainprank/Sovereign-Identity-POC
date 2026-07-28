package com.idtrust.kms.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds {@code idtrust.kms.*} configuration.
 *
 * @param masterKeyBase64 Base64-encoded 256-bit key-encryption-key (KEK) used by
 *                         {@link com.idtrust.kms.crypto.KeyEncryptionService} to envelope-encrypt
 *                         private key material before persistence (FR-KMS-* - "private keys must
 *                         be encrypted before persistence"). The insecure default in
 *                         application.yml is for local development only and must be overridden
 *                         via the KMS_MASTER_KEY_BASE64 environment variable in every non-local
 *                         environment.
 * @param provider         default {@link com.idtrust.kms.provider.KeyProviderType} used when a
 *                         key generation request doesn't specify one explicitly.
 */
@ConfigurationProperties(prefix = "idtrust.kms")
public record KmsProperties(String masterKeyBase64, String provider) {
}
