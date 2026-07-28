package com.idtrust.kms.crypto;

import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.interfaces.EdECPublicKey;
import java.security.spec.EdECPoint;
import java.security.spec.EdECPublicKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.NamedParameterSpec;

/**
 * Converts between the JDK's {@link EdECPublicKey} representation (an {@link EdECPoint} - a
 * BigInteger y-coordinate plus an x-parity bit) and the raw 32-byte little-endian compressed
 * point encoding defined by RFC 8032, which is what did:key's multicodec format requires
 * (multicodec prefix {@code 0xed 0x01} followed directly by the raw 32-byte public key - see
 * {@code com.idtrust.did.provider.didkey.DidKeyProvider}).
 *
 * <p>The JDK does not expose this raw encoding directly ({@link PublicKey#getEncoded()} returns
 * an X.509 DER wrapper instead), so this class implements the RFC 8032 encoding/decoding by hand.
 */
public final class Ed25519Codec {

    private static final int FIELD_BYTES = 32;

    private Ed25519Codec() {
    }

    /** Encodes an Ed25519 public key as its raw 32-byte little-endian compressed point. */
    public static byte[] encodeRawPublicKey(EdECPublicKey publicKey) {
        EdECPoint point = publicKey.getPoint();
        byte[] littleEndianY = toLittleEndian32(point.getY());
        if (point.isXOdd()) {
            littleEndianY[FIELD_BYTES - 1] |= (byte) 0x80;
        }
        return littleEndianY;
    }

    /** Decodes a raw 32-byte little-endian compressed point back into an Ed25519 public key. */
    public static PublicKey decodeRawPublicKey(byte[] rawPublicKey) throws GeneralSecurityException {
        if (rawPublicKey.length != FIELD_BYTES) {
            throw new InvalidKeySpecException("Ed25519 raw public key must be exactly 32 bytes");
        }
        byte[] copy = rawPublicKey.clone();
        boolean xOdd = (copy[FIELD_BYTES - 1] & 0x80) != 0;
        copy[FIELD_BYTES - 1] &= 0x7F;
        reverse(copy);
        BigInteger y = new BigInteger(1, copy);

        KeyFactory keyFactory = KeyFactory.getInstance("Ed25519");
        return keyFactory.generatePublic(
                new EdECPublicKeySpec(NamedParameterSpec.ED25519, new EdECPoint(xOdd, y)));
    }

    private static byte[] toLittleEndian32(BigInteger y) {
        byte[] bigEndian = y.toByteArray();
        // BigInteger may include a leading zero sign byte, or be shorter than 32 bytes; normalize
        // to exactly 32 bytes before reversing to little-endian order.
        byte[] fixed = new byte[FIELD_BYTES];
        int length = Math.min(bigEndian.length, FIELD_BYTES);
        System.arraycopy(bigEndian, bigEndian.length - length, fixed, FIELD_BYTES - length, length);
        reverse(fixed);
        return fixed;
    }

    private static void reverse(byte[] bytes) {
        for (int i = 0, j = bytes.length - 1; i < j; i++, j--) {
            byte tmp = bytes[i];
            bytes[i] = bytes[j];
            bytes[j] = tmp;
        }
    }
}
