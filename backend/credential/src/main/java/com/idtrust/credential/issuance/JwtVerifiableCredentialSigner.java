package com.idtrust.credential.issuance;

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
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Builds and signs a W3C Verifiable Credential as a JWT ("JWT-VC"), the format the current stack
 * targets alongside JSON-LD proofs (per the platform's Confluence source docs). Only Ed25519
 * issuer keys are supported: the JDK's Ed25519 {@code Signature} implementation produces a raw
 * 64-byte signature that is directly JWS-EdDSA-compatible with no DER conversion, unlike
 * ECDSA/RSA - this mirrors E04's decision to only ever generate Ed25519 keys for DID verification
 * methods, so the restriction has no practical impact today.
 */
public final class JwtVerifiableCredentialSigner {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();

    private JwtVerifiableCredentialSigner() {
    }

    public record SignedCredential(String jwt, String credentialHash) {
    }

    public static SignedCredential sign(UUID tenantId, UUID credentialId, Did issuerDid, String subjectDid,
                                         String schemaName, Map<String, Object> claims, Instant expiresAt,
                                         ManagedKey signingKey, KeyService keyService) {
        if (signingKey.getAlgorithm() != KeyAlgorithm.ED25519) {
            throw new ApiException(ApiErrorCode.INVALID_REQUEST,
                    "Only Ed25519 issuer keys can sign credentials, found: " + signingKey.getAlgorithm());
        }

        String kid = assertionMethodId(issuerDid);
        String header = encode(Map.of(
                "alg", "EdDSA",
                "typ", "JWT",
                "kid", kid
        ));

        Map<String, Object> credentialSubject = new LinkedHashMap<>(claims);
        if (subjectDid != null) {
            credentialSubject.put("id", subjectDid);
        }
        Map<String, Object> vc = Map.of(
                "@context", List.of("https://www.w3.org/2018/credentials/v1"),
                "type", List.of("VerifiableCredential", schemaName),
                "credentialSubject", credentialSubject
        );

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
        payloadFields.put("vc", vc);
        String payload = encode(payloadFields);

        String signingInput = header + "." + payload;
        byte[] signature = keyService.sign(tenantId, signingKey.getId(), signingInput.getBytes(StandardCharsets.UTF_8));
        String jwt = signingInput + "." + URL_ENCODER.encodeToString(signature);

        return new SignedCredential(jwt, sha256Hex(jwt));
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

    private static String sha256Hex(String jwt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(jwt.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
