package com.idtrust.kms.provider;

/**
 * Selects which {@link KeyProvider} backend stores/manages a key's material (FR-KMS-* -
 * "provider abstraction: internal encrypted store first, then AWS KMS / Vault / HSM"). Named
 * {@code KeyProviderType} rather than reusing the {@link KeyProvider} name to avoid a Java type
 * collision with the abstraction interface itself.
 */
public enum KeyProviderType {
    INTERNAL,
    AWS_KMS,
    VAULT,
    HSM
}
