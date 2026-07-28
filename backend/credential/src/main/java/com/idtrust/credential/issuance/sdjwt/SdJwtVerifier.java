package com.idtrust.credential.issuance.sdjwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.idtrust.credential.issuance.JwtVerifiableCredentialVerifier;

import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses and verifies an SD-JWT VC "combined format" string ({@code <issuer-signed JWT>~
 * <disclosure 1>~...~<disclosure N>~}) built by {@link SdJwtVcSigner} or filtered down by
 * {@link SdJwtPresentationBuilder}. Covers the Baseline's section 15 verifier-side steps: Signature
 * (delegated to {@link JwtVerifiableCredentialVerifier#verify}, reused as-is since the issuer JWT's
 * header/signing shape is identical to a plain JWT-VC's), and Digest/Disclosure ({@link
 * #verifyDisclosures} - recomputes each disclosure's digest and checks it against the issuer JWT's
 * {@code _sd} claim, so a holder can never inject a claim the issuer never signed for).
 *
 * <p>This class has no DID-resolution dependency of its own, matching {@link
 * JwtVerifiableCredentialVerifier}'s pattern - the caller resolves the issuer DID Document and
 * passes the already-decoded issuer JWT's signature check result separately via {@code
 * JwtVerifiableCredentialVerifier.verify(parsed.issuerJwt(), issuerDocument)}.
 *
 * <p>A trailing Key Binding JWT segment (present when the combined string does not end in
 * {@code ~}) is intentionally not parsed or required - see {@link SdJwtPresentationBuilder}'s
 * Javadoc for why this platform's holder-binding proof lives in the outer VP JWT instead.
 */
public final class SdJwtVerifier {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();

    private SdJwtVerifier() {
    }

    public record ParsedSdJwt(JwtVerifiableCredentialVerifier.DecodedJwt issuerJwt, List<String> disclosures) {
    }

    /** Splits the combined format string into the issuer JWT and its disclosure segments. */
    public static ParsedSdJwt parse(String combined) {
        if (!combined.contains("~")) {
            throw new IllegalArgumentException("Not an SD-JWT combined format string (no '~' separator)");
        }
        boolean noKeyBindingJwt = combined.endsWith("~");
        String[] parts = combined.split("~", -1);

        JwtVerifiableCredentialVerifier.DecodedJwt issuerJwt = JwtVerifiableCredentialVerifier.decode(parts[0]);

        List<String> disclosures = new ArrayList<>();
        for (int i = 1; i < parts.length; i++) {
            String part = parts[i];
            if (part.isEmpty()) {
                continue;
            }
            if (i == parts.length - 1 && !noKeyBindingJwt) {
                // Trailing Key Binding JWT segment - not required/parsed by this platform, see class Javadoc.
                continue;
            }
            disclosures.add(part);
        }
        return new ParsedSdJwt(issuerJwt, disclosures);
    }

    /**
     * Recomputes each disclosure's digest and checks it appears in the issuer JWT's {@code _sd}
     * claim, then expands the verified disclosures into a flat claim-name/claim-value map.
     * Throws {@link IllegalArgumentException} if any disclosure's digest is not found - a genuine
     * integrity failure (a tampered or fabricated disclosure), not a superficial claim comparison.
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> verifyDisclosures(ParsedSdJwt parsed) {
        List<String> sdDigests = (List<String>) parsed.issuerJwt().payload().getOrDefault("_sd", List.of());
        Map<String, Object> claims = new LinkedHashMap<>();
        for (String disclosure : parsed.disclosures()) {
            String digest = SdJwtVcSigner.digest(disclosure);
            if (!sdDigests.contains(digest)) {
                throw new IllegalArgumentException(
                        "SD-JWT disclosure digest not present in issuer's _sd claim - integrity violation");
            }
            List<Object> array = decodeDisclosure(disclosure);
            claims.put((String) array.get(1), array.get(2));
        }
        return claims;
    }

    private static List<Object> decodeDisclosure(String disclosure) {
        try {
            return MAPPER.readValue(URL_DECODER.decode(disclosure), List.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Malformed SD-JWT disclosure", e);
        }
    }
}
