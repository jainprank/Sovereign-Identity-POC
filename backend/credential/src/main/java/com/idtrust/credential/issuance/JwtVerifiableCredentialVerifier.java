package com.idtrust.credential.issuance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.idtrust.did.provider.Base58;
import com.idtrust.kms.crypto.Ed25519Codec;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Verifies the signature of a compact JWT built by {@link JwtVerifiableCredentialSigner} (a
 * JWT-VC) or the analogous VP JWT built by {@code com.idtrust.wallet.oid4vp.WalletPresentationSigner}
 * - both share the same header ({@code alg: EdDSA}, {@code kid} = a DID Document verification
 * method id) and raw-EdDSA-signature JWS shape, so a single verifier covers both (FR-VER-006/007 -
 * Verify Signature/Issuer, FR-VER-009 - Verify Subject Binding, FR-OID4VP-005 - Presentation
 * Signature).
 *
 * <p>This is a genuine cryptographic check, not a superficial claim comparison: it reverses
 * {@code did.provider.Ed25519Multibase}'s encoding by hand (strip the {@code z} multibase prefix,
 * Base58-decode, strip the 2-byte {@code 0xed 0x01} Ed25519 multicodec prefix, then
 * {@link Ed25519Codec#decodeRawPublicKey} to reconstruct a {@link PublicKey}) and verifies with the
 * JDK's {@code Ed25519} {@link Signature} implementation - the same algorithm the signer used, in
 * reverse. The caller is responsible for resolving the signer's DID Document (via
 * {@code did.DidService#resolve}) and passing it in; this class has no DID-resolution dependency of
 * its own so it stays a pure, side-effect-free crypto utility.
 */
public final class JwtVerifiableCredentialVerifier {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();
    private static final byte[] ED25519_MULTICODEC_PREFIX = {(byte) 0xed, 0x01};

    private JwtVerifiableCredentialVerifier() {
    }

    public record DecodedJwt(Map<String, Object> header, Map<String, Object> payload, String signingInput,
                              byte[] signature) {
    }

    /** Splits and base64url-decodes a compact JWT's three segments, without checking the signature. */
    @SuppressWarnings("unchecked")
    public static DecodedJwt decode(String jwt) {
        String[] parts = jwt.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Malformed JWT: expected 3 segments, found " + parts.length);
        }
        try {
            Map<String, Object> header = MAPPER.readValue(URL_DECODER.decode(parts[0]), Map.class);
            Map<String, Object> payload = MAPPER.readValue(URL_DECODER.decode(parts[1]), Map.class);
            byte[] signature = URL_DECODER.decode(parts[2]);
            return new DecodedJwt(header, payload, parts[0] + "." + parts[1], signature);
        } catch (Exception e) {
            throw new IllegalArgumentException("Malformed JWT segment", e);
        }
    }

    /**
     * Verifies a decoded JWT's signature against a signer's DID Document, requiring the JWT
     * header's {@code kid} to be one of the document's {@code assertionMethod} or
     * {@code authentication} verification method ids (defends against a valid signature over a
     * substituted key that the DID's controller never authorized for this purpose).
     */
    @SuppressWarnings("unchecked")
    public static boolean verify(DecodedJwt decoded, Map<String, Object> didDocument) {
        String kid = (String) decoded.header().get("kid");
        if (kid == null) {
            return false;
        }
        List<String> assertionMethods = (List<String>) didDocument.getOrDefault("assertionMethod", List.of());
        List<String> authenticationMethods = (List<String>) didDocument.getOrDefault("authentication", List.of());
        if (!assertionMethods.contains(kid) && !authenticationMethods.contains(kid)) {
            return false;
        }

        List<Map<String, Object>> verificationMethods =
                (List<Map<String, Object>>) didDocument.getOrDefault("verificationMethod", List.of());
        String publicKeyMultibase = verificationMethods.stream()
                .filter(vm -> kid.equals(vm.get("id")))
                .map(vm -> (String) vm.get("publicKeyMultibase"))
                .findFirst()
                .orElse(null);
        if (publicKeyMultibase == null) {
            return false;
        }

        try {
            PublicKey publicKey = decodePublicKey(publicKeyMultibase);
            Signature signature = Signature.getInstance("Ed25519");
            signature.initVerify(publicKey);
            signature.update(decoded.signingInput().getBytes(StandardCharsets.UTF_8));
            return signature.verify(decoded.signature());
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            return false;
        }
    }

    private static PublicKey decodePublicKey(String publicKeyMultibase) throws GeneralSecurityException {
        if (!publicKeyMultibase.startsWith("z")) {
            throw new IllegalArgumentException("Only base58btc (z-prefixed) multibase keys are supported");
        }
        byte[] multicodec = Base58.decode(publicKeyMultibase.substring(1));
        if (multicodec.length <= ED25519_MULTICODEC_PREFIX.length
                || multicodec[0] != ED25519_MULTICODEC_PREFIX[0] || multicodec[1] != ED25519_MULTICODEC_PREFIX[1]) {
            throw new IllegalArgumentException("Not an Ed25519 multicodec-prefixed public key");
        }
        byte[] rawPublicKey = new byte[multicodec.length - ED25519_MULTICODEC_PREFIX.length];
        System.arraycopy(multicodec, ED25519_MULTICODEC_PREFIX.length, rawPublicKey, 0, rawPublicKey.length);
        return Ed25519Codec.decodeRawPublicKey(rawPublicKey);
    }
}
