package com.idtrust.did.provider;

import java.util.Arrays;

/**
 * Base58 (bitcoin alphabet) encode/decode, hand-written to avoid adding an external dependency
 * just for did:key's multibase ({@code z}-prefixed base58btc) identifier encoding. Standard
 * leading-zero-preserving big-integer-in-place-division algorithm (the same approach used by
 * bitcoinj/base58check implementations).
 */
public final class Base58 {

    private static final char[] ALPHABET =
            "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz".toCharArray();
    private static final int[] INDEXES = new int[128];

    static {
        Arrays.fill(INDEXES, -1);
        for (int i = 0; i < ALPHABET.length; i++) {
            INDEXES[ALPHABET[i]] = i;
        }
    }

    private Base58() {
    }

    public static String encode(byte[] input) {
        if (input.length == 0) {
            return "";
        }
        byte[] work = Arrays.copyOf(input, input.length);
        int zeros = 0;
        while (zeros < work.length && work[zeros] == 0) {
            zeros++;
        }

        char[] encoded = new char[work.length * 2];
        int outputStart = encoded.length;
        for (int inputStart = zeros; inputStart < work.length; ) {
            encoded[--outputStart] = ALPHABET[divmod(work, inputStart, 256, 58)];
            if (work[inputStart] == 0) {
                inputStart++;
            }
        }
        while (outputStart < encoded.length && encoded[outputStart] == ALPHABET[0]) {
            outputStart++;
        }
        while (--zeros >= 0) {
            encoded[--outputStart] = ALPHABET[0];
        }
        return new String(encoded, outputStart, encoded.length - outputStart);
    }

    public static byte[] decode(String input) {
        if (input.isEmpty()) {
            return new byte[0];
        }
        byte[] input58 = new byte[input.length()];
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            int digit = c < 128 ? INDEXES[c] : -1;
            if (digit < 0) {
                throw new IllegalArgumentException("Invalid base58 character: " + c);
            }
            input58[i] = (byte) digit;
        }

        int zeros = 0;
        while (zeros < input58.length && input58[zeros] == 0) {
            zeros++;
        }

        byte[] decoded = new byte[input58.length];
        int outputStart = decoded.length;
        for (int inputStart = zeros; inputStart < input58.length; ) {
            decoded[--outputStart] = divmod(input58, inputStart, 58, 256);
            if (input58[inputStart] == 0) {
                inputStart++;
            }
        }
        while (outputStart < decoded.length && decoded[outputStart] == 0) {
            outputStart++;
        }
        return Arrays.copyOfRange(decoded, outputStart - zeros, decoded.length);
    }

    /** Divides the base-{@code fromBase} number in {@code number} (starting at {@code firstDigit}) by {@code toBase}, in place, returning the remainder. */
    private static byte divmod(byte[] number, int firstDigit, int fromBase, int toBase) {
        int remainder = 0;
        for (int i = firstDigit; i < number.length; i++) {
            int digit = number[i] & 0xFF;
            int temp = remainder * fromBase + digit;
            number[i] = (byte) (temp / toBase);
            remainder = temp % toBase;
        }
        return (byte) remainder;
    }
}
