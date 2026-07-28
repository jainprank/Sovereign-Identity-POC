package com.idtrust.apiplatform.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * SHA-256 hashing of bearer-credential plaintext (API keys, OAuth client secrets, OAuth access
 * tokens) before persistence, and generation of the plaintext values themselves. A plain
 * fixed-salt-free hash is sufficient here - unlike a human password, these are high-entropy,
 * randomly generated secrets (never chosen/reused by a person), so dictionary/rainbow-table
 * attacks do not apply the way they do to {@code identity.user}'s password storage.
 */
public final class CredentialHasher {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private CredentialHasher() {
    }

    /** A new, random, URL-safe 256-bit secret, suitable for use directly as a bearer token or client secret. */
    public static String generateSecret() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String hash(String plaintext) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(plaintext.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
