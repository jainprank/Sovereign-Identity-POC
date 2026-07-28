package com.idtrust.kms.crypto;

/**
 * Signing key algorithms supported by the internal {@link com.idtrust.kms.provider.KeyProvider}.
 * The spec (FR-KMS-*) gives only a single example value ({@code "Ed25519"}); these three are the
 * algorithms achievable with JDK-native cryptography (no extra Gradle dependency) and cover the
 * curves used by did:key (Ed25519), typical enterprise PKI (RSA) and blockchain-style tooling
 * (secp256k1).
 */
public enum KeyAlgorithm {
    ED25519,
    SECP256K1,
    RSA_2048
}
