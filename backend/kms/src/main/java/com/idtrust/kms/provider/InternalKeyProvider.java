package com.idtrust.kms.provider;

import com.idtrust.kms.crypto.KeyAlgorithm;
import com.idtrust.kms.crypto.KeyEncryptionService;
import com.idtrust.kms.key.ManagedKey;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

/**
 * Default {@link KeyProvider} backend (FR-KMS-* - "internal encrypted store first"): generates
 * keys with JDK-native cryptography and persists private key material envelope-encrypted via
 * {@link KeyEncryptionService}. Every {@link KeyAlgorithm} is achievable without an extra Gradle
 * dependency: Ed25519 and RSA are supported directly by the JDK, and secp256k1 is supported by
 * the JDK's built-in SunEC provider via {@link ECGenParameterSpec}.
 */
@Component
public class InternalKeyProvider implements KeyProvider {

    private final KeyEncryptionService keyEncryptionService;

    public InternalKeyProvider(KeyEncryptionService keyEncryptionService) {
        this.keyEncryptionService = keyEncryptionService;
    }

    @Override
    public KeyProviderType type() {
        return KeyProviderType.INTERNAL;
    }

    @Override
    public GeneratedKeyMaterial generateKey(KeyAlgorithm algorithm) {
        try {
            KeyPair keyPair = generateKeyPair(algorithm);
            String encryptedPrivateKey = keyEncryptionService.encrypt(keyPair.getPrivate().getEncoded());
            return new GeneratedKeyMaterial(keyPair.getPublic().getEncoded(), encryptedPrivateKey, null);
        } catch (NoSuchAlgorithmException | java.security.InvalidAlgorithmParameterException e) {
            throw new IllegalStateException("Failed to generate " + algorithm + " key pair", e);
        }
    }

    @Override
    public byte[] sign(ManagedKey key, byte[] payload) {
        try {
            PrivateKey privateKey = decodePrivateKey(key);
            Signature signature = Signature.getInstance(signatureAlgorithm(key.getAlgorithm()));
            signature.initSign(privateKey);
            signature.update(payload);
            return signature.sign();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to sign payload with key " + key.getId(), e);
        }
    }

    @Override
    public void deactivate(ManagedKey key) {
        // Internal store keeps the (still-encrypted) private key material even once deactivated,
        // so past signatures/credentials remain verifiable; the ManagedKey.status flag is the
        // enforcement point that prevents further signing.
    }

    @Override
    public void delete(ManagedKey key) {
        // No external resource to release for the internal store; deletion of the ManagedKey row
        // itself (and therefore its encrypted private key) is handled by the repository layer.
    }

    @Override
    public boolean healthCheck() {
        return true;
    }

    private KeyPair generateKeyPair(KeyAlgorithm algorithm)
            throws NoSuchAlgorithmException, java.security.InvalidAlgorithmParameterException {
        return switch (algorithm) {
            case ED25519 -> KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
            case SECP256K1 -> {
                KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
                generator.initialize(new ECGenParameterSpec("secp256k1"));
                yield generator.generateKeyPair();
            }
            case RSA_2048 -> {
                KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
                generator.initialize(2048);
                yield generator.generateKeyPair();
            }
        };
    }

    private PrivateKey decodePrivateKey(ManagedKey key) throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] pkcs8 = keyEncryptionService.decrypt(key.getEncryptedPrivateKey());
        KeyFactory keyFactory = KeyFactory.getInstance(keyFactoryAlgorithm(key.getAlgorithm()));
        return keyFactory.generatePrivate(new PKCS8EncodedKeySpec(pkcs8));
    }

    private String keyFactoryAlgorithm(KeyAlgorithm algorithm) {
        return switch (algorithm) {
            case ED25519 -> "Ed25519";
            case SECP256K1 -> "EC";
            case RSA_2048 -> "RSA";
        };
    }

    private String signatureAlgorithm(KeyAlgorithm algorithm) {
        return switch (algorithm) {
            case ED25519 -> "Ed25519";
            case SECP256K1 -> "SHA256withECDSA";
            case RSA_2048 -> "SHA256withRSA";
        };
    }
}
