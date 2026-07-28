package com.idtrust.kms.crypto;

import com.idtrust.kms.config.KmsProperties;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Envelope-encrypts private key material before it is persisted (FR-KMS-* - "private keys must
 * be encrypted before persistence"). Uses AES/GCM/NoPadding with a single master
 * key-encryption-key (KEK) sourced from {@link KmsProperties#masterKeyBase64()} - the simplest
 * scheme that satisfies "encrypted at rest" for the internal provider; a KMS-backed KEK hierarchy
 * (e.g. AWS KMS-wrapped data keys) can replace this later without changing the
 * {@link com.idtrust.kms.provider.KeyProvider} contract.
 *
 * <p>Ciphertext layout is {@code base64(iv[12] || ciphertext || authTag[16])}: a fresh random IV
 * is generated per encryption and prepended, so the same plaintext never produces the same
 * ciphertext twice and no IV needs to be tracked separately.
 */
@Component
public class KeyEncryptionService {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH_BYTES = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private final SecretKeySpec masterKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public KeyEncryptionService(KmsProperties kmsProperties) {
        byte[] keyBytes = Base64.getDecoder().decode(kmsProperties.masterKeyBase64());
        this.masterKey = new SecretKeySpec(keyBytes, "AES");
    }

    public String encrypt(byte[] plaintext) {
        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, masterKey, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext);

            byte[] combined = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to encrypt key material", e);
        }
    }

    public byte[] decrypt(String encoded) {
        try {
            byte[] combined = Base64.getDecoder().decode(encoded);
            byte[] iv = new byte[IV_LENGTH_BYTES];
            byte[] ciphertext = new byte[combined.length - IV_LENGTH_BYTES];
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH_BYTES);
            System.arraycopy(combined, IV_LENGTH_BYTES, ciphertext, 0, ciphertext.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, masterKey, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            return cipher.doFinal(ciphertext);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to decrypt key material", e);
        }
    }
}
