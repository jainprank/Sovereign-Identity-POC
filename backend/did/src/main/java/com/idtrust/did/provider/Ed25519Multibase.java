package com.idtrust.did.provider;

import com.idtrust.kms.crypto.Ed25519Codec;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.interfaces.EdECPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Encodes an Ed25519 public key (as stored by {@code com.idtrust.kms.key.ManagedKey}, Base64 of
 * X.509 DER) into the multibase string used both by did:key's identifier itself and by both
 * methods' {@code publicKeyMultibase} verification method field: multicodec prefix
 * {@code 0xed 0x01} + raw 32-byte public key, base58btc-encoded with a {@code z} prefix. Shared by
 * {@link com.idtrust.did.provider.didkey.DidKeyProvider} and
 * {@link com.idtrust.did.provider.didweb.DidWebProvider} since both use the same encoding for
 * their verification methods (see {@link Ed25519DidDocuments}).
 */
public final class Ed25519Multibase {

    private static final byte[] ED25519_MULTICODEC_PREFIX = {(byte) 0xed, 0x01};

    private Ed25519Multibase() {
    }

    public static String encode(String base64X509PublicKey) {
        try {
            byte[] x509 = Base64.getDecoder().decode(base64X509PublicKey);
            KeyFactory keyFactory = KeyFactory.getInstance("Ed25519");
            PublicKey publicKey = keyFactory.generatePublic(new X509EncodedKeySpec(x509));
            byte[] rawPublicKey = Ed25519Codec.encodeRawPublicKey((EdECPublicKey) publicKey);

            byte[] multicodec = new byte[ED25519_MULTICODEC_PREFIX.length + rawPublicKey.length];
            System.arraycopy(ED25519_MULTICODEC_PREFIX, 0, multicodec, 0, ED25519_MULTICODEC_PREFIX.length);
            System.arraycopy(rawPublicKey, 0, multicodec, ED25519_MULTICODEC_PREFIX.length, rawPublicKey.length);
            return "z" + Base58.encode(multicodec);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to derive Ed25519 multibase from public key", e);
        }
    }
}
