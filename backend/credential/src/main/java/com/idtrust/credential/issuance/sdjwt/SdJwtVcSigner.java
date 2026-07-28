package com.idtrust.credential.issuance.sdjwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.idtrust.common.web.ApiErrorCode;
import com.idtrust.common.web.ApiException;
import com.idtrust.did.Did;
import com.idtrust.kms.crypto.KeyAlgorithm;
import com.idtrust.kms.key.KeyService;
import com.idtrust.kms.key.ManagedKey;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Builds and signs an SD-JWT VC (EPIC 6/FR-ISS-008, applied to {@code CredentialFormat.SD_JWT_VC}
 * schemas) per the Baseline's section 15 "SD-JWT Selective Disclosure" flow (claims salted,
 * digests generated, issuer signs). Structurally the SD-JWT sibling of
 * {@link com.idtrust.credential.issuance.JwtVerifiableCredentialSigner} - same header/signing
 * shape ({@code alg: EdDSA}, {@code kid} = issuer DID's {@code assertionMethod[0]}, raw-EdDSA
 * JWS) - except each schema claim becomes an individually salted, hashed "disclosure" rather than
 * an inline {@code credentialSubject} field, so a holder can later reveal only a subset of them
 * (FR-WAL-014).
 *
 * <p><b>Scope decision:</b> only the schema-defined claims are selectively disclosable. {@code sub}
 * (subject DID) stays an always-visible top-level payload claim, not a digest - the platform's
 * subject-binding check (FR-VER-009) and the wallet's own credential lookups need it without first
 * having to open a disclosure, and hiding the subject DID is not a goal of any FR here (unlike
 * hiding claim values, which is FR-WAL-014's actual concern).
 *
 * <p>Digests are sorted before being placed in {@code _sd} (rather than left in claim-insertion
 * order) so the order of the array itself never leaks which claim was added first - the digests'
 * random salts already make the values unguessable, but a fixed order is an unnecessary residual
 * signal.
 *
 * <p>Output is the SD-JWT "combined format" string: {@code <issuer-signed JWT>~<disclosure 1>~
 * ...~<disclosure N>~} (trailing {@code ~}, no Key Binding JWT at issuance time - see
 * {@link SdJwtPresentationBuilder}'s Javadoc for why this platform's architecture doesn't add one
 * at presentation time either).
 */
public final class SdJwtVcSigner {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final SecureRandom RANDOM = new SecureRandom();

    private SdJwtVcSigner() {
    }

    public record SignedSdJwt(String combined, String credentialHash) {
    }

    public static SignedSdJwt sign(UUID tenantId, UUID credentialId, Did issuerDid, String subjectDid,
                                    String vct, Map<String, Object> claims, Instant expiresAt,
                                    ManagedKey signingKey, KeyService keyService) {
        if (signingKey.getAlgorithm() != KeyAlgorithm.ED25519) {
            throw new ApiException(ApiErrorCode.INVALID_REQUEST,
                    "Only Ed25519 issuer keys can sign credentials, found: " + signingKey.getAlgorithm());
        }

        List<String> disclosures = new ArrayList<>();
        List<String> digests = new ArrayList<>();
        for (Map.Entry<String, Object> claim : claims.entrySet()) {
            String disclosure = buildDisclosure(claim.getKey(), claim.getValue());
            disclosures.add(disclosure);
            digests.add(digest(disclosure));
        }
        digests.sort(String::compareTo);

        String kid = assertionMethodId(issuerDid);
        String header = encode(Map.of(
                "alg", "EdDSA",
                "typ", "vc+sd-jwt",
                "kid", kid
        ));

        Map<String, Object> payloadFields = new LinkedHashMap<>();
        payloadFields.put("iss", issuerDid.getDid());
        if (subjectDid != null) {
            payloadFields.put("sub", subjectDid);
        }
        payloadFields.put("iat", Instant.now().getEpochSecond());
        if (expiresAt != null) {
            payloadFields.put("exp", expiresAt.getEpochSecond());
        }
        payloadFields.put("jti", "urn:uuid:" + credentialId);
        payloadFields.put("vct", vct);
        payloadFields.put("_sd_alg", "sha-256");
        payloadFields.put("_sd", digests);
        String payload = encode(payloadFields);

        String signingInput = header + "." + payload;
        byte[] signature = keyService.sign(tenantId, signingKey.getId(), signingInput.getBytes(StandardCharsets.UTF_8));
        String jwt = signingInput + "." + URL_ENCODER.encodeToString(signature);

        StringBuilder combined = new StringBuilder(jwt);
        for (String disclosure : disclosures) {
            combined.append('~').append(disclosure);
        }
        combined.append('~');

        return new SignedSdJwt(combined.toString(), digest(combined.toString()));
    }

    /** Builds one {@code [salt, claimName, claimValue]} disclosure, base64url-encoded per the SD-JWT spec. */
    static String buildDisclosure(String claimName, Object claimValue) {
        byte[] saltBytes = new byte[16];
        RANDOM.nextBytes(saltBytes);
        String salt = URL_ENCODER.encodeToString(saltBytes);
        List<Object> array = List.of(salt, claimName, claimValue == null ? "" : claimValue);
        try {
            return URL_ENCODER.encodeToString(MAPPER.writeValueAsBytes(array));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize SD-JWT disclosure", e);
        }
    }

    /** The base64url-encoded SHA-256 digest of a disclosure string, as required for {@code _sd} entries. */
    static String digest(String disclosure) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            return URL_ENCODER.encodeToString(sha256.digest(disclosure.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    /** The issuer DID's assertionMethod verification method id, used as the JWT {@code kid}. */
    @SuppressWarnings("unchecked")
    private static String assertionMethodId(Did issuerDid) {
        Object assertionMethod = issuerDid.getDocument().get("assertionMethod");
        if (!(assertionMethod instanceof List<?> list) || list.isEmpty()) {
            throw new ApiException(ApiErrorCode.INVALID_REQUEST,
                    "Issuer DID has no assertionMethod verification method: " + issuerDid.getDid());
        }
        return (String) list.get(0);
    }

    private static String encode(Map<String, Object> value) {
        try {
            return URL_ENCODER.encodeToString(MAPPER.writeValueAsBytes(value));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize JWT segment", e);
        }
    }
}
