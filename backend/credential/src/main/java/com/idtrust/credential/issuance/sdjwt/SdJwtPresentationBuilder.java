package com.idtrust.credential.issuance.sdjwt;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Base64;
import java.util.List;

/**
 * Derives a holder presentation of an SD-JWT VC (FR-WAL-013/014 - Select Credential, Selective
 * Disclosure) by keeping the issuer-signed JWT untouched and dropping every disclosure whose
 * claim name the holder did not choose to reveal - the verifier only ever sees the issuer's
 * signature plus the disclosures actually present, exactly satisfying the Baseline's section 15
 * requirement that "the verifier must not receive undisclosed claims".
 *
 * <p><b>No Key Binding JWT is appended here</b>, unlike the canonical SD-JWT VC presentation
 * format. This platform's OID4VP architecture (built in E10) already wraps every presented
 * credential artifact - SD-JWT or plain JWT-VC alike - inside a holder-signed outer VP JWT
 * ({@code wallet.oid4vp.WalletPresentationSigner}) carrying {@code iss}/{@code aud}/{@code nonce},
 * which already proves holder possession of the credential's subject key. A second, inner
 * KB-JWT would prove the exact same thing a second time; skipping it is a documented
 * simplification, not a missing security control.
 */
public final class SdJwtPresentationBuilder {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();

    private SdJwtPresentationBuilder() {
    }

    /**
     * Returns a new combined-format string keeping the issuer JWT and only the disclosures whose
     * claim name is in {@code disclosedClaimNames}. A {@code null} or empty list discloses every
     * claim the issuer made selectively disclosable (full disclosure, the default when the holder
     * doesn't request selective disclosure).
     */
    public static String filterDisclosures(String issuerCombined, List<String> disclosedClaimNames) {
        SdJwtVerifier.ParsedSdJwt parsed = SdJwtVerifier.parse(issuerCombined);
        String issuerJwtPart = issuerCombined.split("~", -1)[0];

        StringBuilder combined = new StringBuilder(issuerJwtPart);
        for (String disclosure : parsed.disclosures()) {
            String claimName = decodeClaimName(disclosure);
            if (disclosedClaimNames == null || disclosedClaimNames.isEmpty() || disclosedClaimNames.contains(claimName)) {
                combined.append('~').append(disclosure);
            }
        }
        combined.append('~');
        return combined.toString();
    }

    @SuppressWarnings("unchecked")
    private static String decodeClaimName(String disclosure) {
        try {
            List<Object> array = MAPPER.readValue(URL_DECODER.decode(disclosure), List.class);
            return (String) array.get(1);
        } catch (Exception e) {
            throw new IllegalArgumentException("Malformed SD-JWT disclosure", e);
        }
    }
}
