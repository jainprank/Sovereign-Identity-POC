package com.idtrust.wallet.oid4vp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.idtrust.common.web.ApiErrorCode;
import com.idtrust.common.web.ApiException;
import com.idtrust.did.Did;
import com.idtrust.kms.crypto.KeyAlgorithm;
import com.idtrust.kms.key.KeyService;
import com.idtrust.kms.key.ManagedKey;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Builds and signs a Verifiable Presentation as a JWT ("VP JWT") for FR-WAL-011 (Select
 * Credentials for Presentation) / FR-WAL-012 (Generate Presentation) / FR-OID4VP-004
 * (Presentation Response). Structurally the mirror image of {@code credential.issuance.
 * JwtVerifiableCredentialSigner}'s VC JWT - same header shape ({@code kid} = the holder DID's
 * {@code assertionMethod[0]}), same Ed25519-only raw-JWS-signature restriction - except the
 * payload wraps a {@code vp.verifiableCredential} array of already-signed VC JWTs instead of an
 * inline {@code vc.credentialSubject}, and carries {@code aud}/{@code nonce} for the verifier to
 * check ({@code com.idtrust.verification.VerificationService#presentAndVerify}) rather than an
 * issuer's own claims.
 *
 * <p>This gives genuine cryptographic subject-binding proof (FR-VER-009): signing with the
 * holder's own KMS-backed key demonstrates possession of the DID named in the embedded
 * credential's {@code sub} claim, unlike a superficial claim-matching check.
 */
public final class WalletPresentationSigner {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();

    private WalletPresentationSigner() {
    }

    public record SignedPresentation(String jwt) {
    }

    public static SignedPresentation sign(UUID tenantId, Did holderDid, String audience, String nonce,
                                           List<String> credentialJwts, Instant expiresAt,
                                           ManagedKey signingKey, KeyService keyService) {
        if (signingKey.getAlgorithm() != KeyAlgorithm.ED25519) {
            throw new ApiException(ApiErrorCode.INVALID_REQUEST,
                    "Only Ed25519 holder keys can sign presentations, found: " + signingKey.getAlgorithm());
        }

        String kid = assertionMethodId(holderDid);
        String header = encode(Map.of(
                "alg", "EdDSA",
                "typ", "JWT",
                "kid", kid
        ));

        Map<String, Object> vp = Map.of(
                "@context", List.of("https://www.w3.org/2018/credentials/v1"),
                "type", List.of("VerifiablePresentation"),
                "verifiableCredential", credentialJwts
        );

        Map<String, Object> payloadFields = new LinkedHashMap<>();
        payloadFields.put("iss", holderDid.getDid());
        payloadFields.put("aud", audience);
        payloadFields.put("nonce", nonce);
        payloadFields.put("iat", Instant.now().getEpochSecond());
        payloadFields.put("exp", expiresAt.getEpochSecond());
        payloadFields.put("vp", vp);
        String payload = encode(payloadFields);

        String signingInput = header + "." + payload;
        byte[] signature = keyService.sign(tenantId, signingKey.getId(), signingInput.getBytes(StandardCharsets.UTF_8));
        String jwt = signingInput + "." + URL_ENCODER.encodeToString(signature);

        return new SignedPresentation(jwt);
    }

    /** The holder DID's assertionMethod verification method id, used as the VP JWT {@code kid}. */
    @SuppressWarnings("unchecked")
    private static String assertionMethodId(Did holderDid) {
        Object assertionMethod = holderDid.getDocument().get("assertionMethod");
        if (!(assertionMethod instanceof List<?> list) || list.isEmpty()) {
            throw new ApiException(ApiErrorCode.INVALID_REQUEST,
                    "Holder DID has no assertionMethod verification method: " + holderDid.getDid());
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
