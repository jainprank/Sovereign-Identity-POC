package com.idtrust.credential.statuslist;

import com.idtrust.common.web.ApiErrorCode;
import com.idtrust.common.web.ApiException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * GZIP + Base64url bit manipulation for a StatusList2021-shaped bitstring, matching the spec's own
 * encoding ({@code https://www.w3.org/TR/vc-bitstring-status-list/#bitstring-encoding}). One bit per
 * credential; a set bit means revoked.
 */
public final class BitstringCodec {

    private BitstringCodec() {
    }

    public static String empty(int sizeInBits) {
        return encode(new byte[byteLength(sizeInBits)]);
    }

    public static boolean getBit(String encoded, int index) {
        byte[] bits = decode(encoded);
        checkIndex(bits, index);
        int byteIndex = index / 8;
        int bitOffset = index % 8;
        return (bits[byteIndex] & (1 << bitOffset)) != 0;
    }

    public static String setBit(String encoded, int index, boolean value) {
        byte[] bits = decode(encoded);
        checkIndex(bits, index);
        int byteIndex = index / 8;
        int bitOffset = index % 8;
        if (value) {
            bits[byteIndex] |= (byte) (1 << bitOffset);
        } else {
            bits[byteIndex] &= (byte) ~(1 << bitOffset);
        }
        return encode(bits);
    }

    private static void checkIndex(byte[] bits, int index) {
        if (index < 0 || index / 8 >= bits.length) {
            throw new ApiException(ApiErrorCode.INVALID_REQUEST, "Status list index out of range: " + index);
        }
    }

    private static int byteLength(int sizeInBits) {
        return (sizeInBits + 7) / 8;
    }

    private static String encode(byte[] bits) {
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            try (GZIPOutputStream gzip = new GZIPOutputStream(buffer)) {
                gzip.write(bits);
            }
            return Base64.getUrlEncoder().withoutPadding().encodeToString(buffer.toByteArray());
        } catch (IOException e) {
            throw new ApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to encode status list bitstring");
        }
    }

    private static byte[] decode(String encoded) {
        try {
            byte[] gzipped = Base64.getUrlDecoder().decode(encoded);
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(gzipped))) {
                gzip.transferTo(buffer);
            }
            return buffer.toByteArray();
        } catch (IOException e) {
            throw new ApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to decode status list bitstring");
        }
    }
}
