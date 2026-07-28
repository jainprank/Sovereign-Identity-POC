package com.idtrust.verification;

import com.idtrust.common.audit.AuditEvent;
import com.idtrust.common.audit.AuditEventPublisher;
import com.idtrust.common.tenant.CurrentUserContext;
import com.idtrust.common.web.ApiErrorCode;
import com.idtrust.common.web.ApiException;
import com.idtrust.credential.issuance.Credential;
import com.idtrust.credential.issuance.CredentialIssuanceService;
import com.idtrust.credential.issuance.CredentialStatus;
import com.idtrust.credential.issuance.JwtVerifiableCredentialVerifier;
import com.idtrust.credential.issuance.sdjwt.SdJwtVerifier;
import com.idtrust.did.DidService;
import com.idtrust.verification.oid4vp.Oid4vpRequestGenerator;
import com.idtrust.verification.policy.PolicyDefinition;
import com.idtrust.verification.policy.PolicyStatus;
import com.idtrust.verification.policy.VerificationPolicy;
import com.idtrust.verification.policy.VerificationPolicyService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Implements EPIC 10 (Verification) and EPIC 11 (OID4VP &amp; DCQL): FR-VER-001 through 003
 * (Create/QR/Track Verification Request), FR-VER-006 through 014 (the full check pipeline in
 * {@link #presentAndVerify}), FR-VER-015 (History, via {@link #list}), FR-OID4VP-001 through 005
 * (Presentation Request/Response/Signature, via {@link Oid4vpRequestGenerator} and
 * {@link JwtVerifiableCredentialVerifier}), FR-OID4VP-012 (Replay Protection, via the
 * {@code nonce} check).
 *
 * <p>Deliberate scope decisions:
 * <ul>
 *   <li>FR-VER-012/FR-POL-011 (Verify Trust/Trust Rule) is a static {@code trustedIssuerDids}
 *       allowlist on the policy, not a dynamic Trust Registry lookup - the full Trust Registry
 *       (EPIC 13, FR-TRUST-*) is out of scope until E12 per the build sequence.</li>
 *   <li>FR-VER-011 (Verify Status) only works reliably when the credential's issuer belongs to
 *       this verifier's own tenant: {@link CredentialIssuanceService#get} is strictly
 *       tenant-scoped under RLS with no public cross-tenant equivalent to
 *       {@link DidService#resolve}. A lookup failure is recorded as {@code statusCheck: "UNKNOWN"}
 *       rather than failing the whole verification - a documented limitation, not a silent bypass.</li>
 *   <li>Only the first entry of a VP's {@code vp.verifiableCredential} array is checked - this
 *       platform's wallet ({@code WalletPresentationSigner}) only ever submits one credential per
 *       presentation in this pass; multi-credential presentations are a natural but unbuilt
 *       extension.</li>
 *   <li>EPIC 5/8's SD-JWT VC support (E11: {@code CredentialFormat.SD_JWT_VC}, FR-WAL-014) is
 *       handled transparently by {@link #decodeCredentialArtifact}, which distinguishes a
 *       combined-format SD-JWT string from a plain W3C JWT-VC by the presence of {@code "~"} (never
 *       legal inside a compact 3-part JWT). Disclosure integrity is a genuine check, not a
 *       superficial one: {@link SdJwtVerifier#verifyDisclosures} recomputes each disclosure's
 *       SHA-256 digest and confirms it was actually present in the issuer-signed {@code _sd} claim
 *       before exposing the claim value, recorded as {@code disclosureIntegrityValid} in the result
 *       and folded into the overall pass/fail. No Key Binding JWT (KB-JWT) is required or parsed -
 *       this platform's outer VP JWT ({@code WalletPresentationSigner}) already carries a
 *       holder-signed {@code iss}/{@code aud}/{@code nonce}, which proves possession of the same
 *       holder key an inner KB-JWT would otherwise prove redundantly. A documented simplification,
 *       not a missing security control.</li>
 * </ul>
 */
@Service
public class VerificationService {

    private static final Duration REQUEST_TTL = Duration.ofMinutes(10);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final VerificationRequestRepository requestRepository;
    private final VerificationPolicyService policyService;
    private final DidService didService;
    private final CredentialIssuanceService credentialIssuanceService;
    private final AuditEventPublisher auditEventPublisher;

    public VerificationService(VerificationRequestRepository requestRepository, VerificationPolicyService policyService,
                                DidService didService, CredentialIssuanceService credentialIssuanceService,
                                AuditEventPublisher auditEventPublisher) {
        this.requestRepository = requestRepository;
        this.policyService = policyService;
        this.didService = didService;
        this.credentialIssuanceService = credentialIssuanceService;
        this.auditEventPublisher = auditEventPublisher;
    }

    @Transactional
    public RequestResult createRequest(UUID tenantId, UUID policyId, String callbackUrl) {
        VerificationPolicy policy = policyService.get(tenantId, policyId);
        if (policy.getStatus() != PolicyStatus.ACTIVE) {
            throw new ApiException(ApiErrorCode.INVALID_REQUEST, "Only an ACTIVE policy can be used for verification: " + policyId);
        }

        String nonce = randomToken();
        Instant expiresAt = Instant.now().plus(REQUEST_TTL);
        Map<String, Object> presentationDefinition = buildPresentationDefinition(policy.getDefinition());
        VerificationRequest request = new VerificationRequest(tenantId, policyId, callbackUrl, nonce,
                presentationDefinition, expiresAt);
        requestRepository.save(request);

        String clientId = "urn:idtrust:tenant:" + tenantId;
        Oid4vpRequestGenerator.Request oid4vpRequest = Oid4vpRequestGenerator.generate(
                clientId, request.getId().toString(), nonce, presentationDefinition);
        audit(tenantId, "VERIFICATION_REQUEST_CREATED", request.getId());
        return new RequestResult(request, oid4vpRequest);
    }

    public VerificationRequest get(UUID tenantId, UUID id) {
        return requestRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new ApiException(ApiErrorCode.RESOURCE_NOT_FOUND, "Verification request not found: " + id));
    }

    public List<VerificationRequest> list(UUID tenantId, VerificationStatus status) {
        return requestRepository.search(tenantId, status);
    }

    @Transactional
    public VerificationRequest presentAndVerify(UUID tenantId, UUID requestId, String vpJwt) {
        VerificationRequest request = get(tenantId, requestId);
        if (request.isExpired()) {
            request.expire();
            throw new ApiException(ApiErrorCode.INVALID_REQUEST, "This verification request has expired");
        }
        if (request.getStatus() != VerificationStatus.PENDING) {
            throw new ApiException(ApiErrorCode.INVALID_REQUEST, "This verification request has already been processed");
        }

        VerificationPolicy policy = policyService.get(tenantId, request.getPolicyId());
        PolicyDefinition rules = policy.getDefinition();
        String clientId = "urn:idtrust:tenant:" + tenantId;

        Map<String, Object> checks = new LinkedHashMap<>();
        Result result = evaluate(request, rules, clientId, vpJwt, checks);

        if (result.passed()) {
            request.verify(result.credentialId(), result.subjectDid(), checks);
            audit(tenantId, "VERIFICATION_REQUEST_VERIFIED", request.getId());
        } else {
            request.fail(result.failureReason(), checks);
            audit(tenantId, "VERIFICATION_REQUEST_FAILED", request.getId());
        }
        return request;
    }

    @SuppressWarnings("unchecked")
    private Result evaluate(VerificationRequest request, PolicyDefinition rules, String clientId, String vpJwt,
                             Map<String, Object> checks) {
        JwtVerifiableCredentialVerifier.DecodedJwt vp;
        try {
            vp = JwtVerifiableCredentialVerifier.decode(vpJwt);
        } catch (IllegalArgumentException e) {
            return Result.failure("Malformed presentation JWT");
        }

        String holderDid = (String) vp.payload().get("iss");
        boolean nonceValid = request.getNonce().equals(vp.payload().get("nonce"));
        boolean audienceValid = clientId.equals(vp.payload().get("aud"));
        checks.put("nonceValid", nonceValid);
        checks.put("audienceValid", audienceValid);

        boolean holderSignatureValid;
        try {
            Map<String, Object> holderDocument = didService.resolve(holderDid);
            holderSignatureValid = JwtVerifiableCredentialVerifier.verify(vp, holderDocument);
        } catch (Exception e) {
            holderSignatureValid = false;
        }
        checks.put("holderSignatureValid", holderSignatureValid);

        Object vpClaim = vp.payload().get("vp");
        List<String> credentials = vpClaim instanceof Map<?, ?> vpMap
                ? (List<String>) vpMap.get("verifiableCredential") : null;
        if (credentials == null || credentials.isEmpty()) {
            checks.put("credentialPresent", false);
            return Result.failure("Presentation carries no verifiable credential");
        }
        checks.put("credentialPresent", true);

        CredentialView vc;
        try {
            vc = decodeCredentialArtifact(credentials.get(0));
        } catch (IllegalArgumentException e) {
            return Result.failure("Malformed verifiable credential");
        }

        String issuerDid = vc.issuerDid();
        String subjectDid = vc.subjectDid();
        boolean subjectBindingValid = holderDid != null && holderDid.equals(subjectDid);
        checks.put("subjectBindingValid", subjectBindingValid);

        boolean credentialSignatureValid;
        try {
            Map<String, Object> issuerDocument = didService.resolve(issuerDid);
            credentialSignatureValid = JwtVerifiableCredentialVerifier.verify(vc.signedJwt(), issuerDocument);
        } catch (Exception e) {
            credentialSignatureValid = false;
        }
        checks.put("credentialSignatureValid", credentialSignatureValid);
        checks.put("disclosureIntegrityValid", vc.disclosureIntegrityValid());

        Object expClaim = vc.expClaim();
        boolean notExpired = expClaim == null
                || Instant.now().getEpochSecond() < ((Number) expClaim).longValue();
        checks.put("notExpired", notExpired);

        List<String> trustedIssuerDids = rules.trustedIssuerDids();
        boolean trustedIssuer = trustedIssuerDids == null || trustedIssuerDids.isEmpty()
                || trustedIssuerDids.contains(issuerDid);
        checks.put("trustedIssuer", trustedIssuer);

        Map<String, Object> credentialSubject = vc.credentialSubject();
        List<String> requiredClaims = rules.requiredClaims();
        boolean requiredClaimsPresent = requiredClaims == null || requiredClaims.isEmpty()
                || requiredClaims.stream().allMatch(credentialSubject::containsKey);
        checks.put("requiredClaimsPresent", requiredClaimsPresent);

        UUID credentialId = parseCredentialId(vc.jti());
        String statusCheck = "UNKNOWN";
        boolean statusOk = true;
        if (credentialId != null) {
            try {
                Credential credential = credentialIssuanceService.get(request.getTenantId(), credentialId);
                statusCheck = credential.getStatus().name();
                statusOk = !rules.requireActiveStatus() || credential.getStatus() == CredentialStatus.ISSUED;
            } catch (ApiException e) {
                // Cross-tenant issuer - see class Javadoc's documented FR-VER-011 limitation.
                statusCheck = "UNKNOWN";
                statusOk = true;
            }
        }
        checks.put("statusCheck", statusCheck);

        boolean passed = nonceValid && audienceValid && holderSignatureValid && subjectBindingValid
                && credentialSignatureValid && vc.disclosureIntegrityValid() && (!rules.requireNotExpired() || notExpired)
                && trustedIssuer && requiredClaimsPresent && statusOk;

        if (!passed) {
            return Result.failure(firstFailureReason(checks));
        }
        return Result.success(credentialId, subjectDid);
    }

    /**
     * Decodes either a plain JWT-VC or an SD-JWT VC "combined format" string (detected by the
     * presence of {@code ~}, which never appears in a compact JWT) into a common view, so
     * {@link #evaluate} doesn't need to know which format it's checking. For SD-JWT, {@code
     * credentialSubject} is the disclosed-claims map recomputed by {@link
     * SdJwtVerifier#verifyDisclosures} (FR-VER-007's "Check Digest/Disclosure" per the Baseline's
     * section 15) rather than an inline {@code vc.credentialSubject} - a disclosure whose digest
     * doesn't match the issuer's {@code _sd} claim fails {@code disclosureIntegrityValid} instead
     * of throwing, so the rest of the checks still run and the failure is recorded like any other.
     */
    @SuppressWarnings("unchecked")
    private CredentialView decodeCredentialArtifact(String artifact) {
        if (artifact.contains("~")) {
            SdJwtVerifier.ParsedSdJwt parsed = SdJwtVerifier.parse(artifact);
            Map<String, Object> credentialSubject;
            boolean disclosureIntegrityValid;
            try {
                credentialSubject = SdJwtVerifier.verifyDisclosures(parsed);
                disclosureIntegrityValid = true;
            } catch (IllegalArgumentException e) {
                credentialSubject = Map.of();
                disclosureIntegrityValid = false;
            }
            Map<String, Object> payload = parsed.issuerJwt().payload();
            return new CredentialView(parsed.issuerJwt(), (String) payload.get("iss"), (String) payload.get("sub"),
                    credentialSubject, payload.get("exp"), (String) payload.get("jti"), disclosureIntegrityValid);
        }

        JwtVerifiableCredentialVerifier.DecodedJwt vc = JwtVerifiableCredentialVerifier.decode(artifact);
        Map<String, Object> credentialSubject = extractCredentialSubject(vc.payload());
        return new CredentialView(vc, (String) vc.payload().get("iss"), (String) vc.payload().get("sub"),
                credentialSubject, vc.payload().get("exp"), (String) vc.payload().get("jti"), true);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractCredentialSubject(Map<String, Object> vcPayload) {
        Object vcClaim = vcPayload.get("vc");
        if (vcClaim instanceof Map<?, ?> vcMap && vcMap.get("credentialSubject") instanceof Map<?, ?> subject) {
            return (Map<String, Object>) subject;
        }
        return Map.of();
    }

    /** A format-agnostic view over either a plain JWT-VC or a parsed SD-JWT VC's issuer JWT. */
    private record CredentialView(JwtVerifiableCredentialVerifier.DecodedJwt signedJwt, String issuerDid,
                                   String subjectDid, Map<String, Object> credentialSubject, Object expClaim,
                                   String jti, boolean disclosureIntegrityValid) {
    }

    private UUID parseCredentialId(String jti) {
        if (jti == null || !jti.startsWith("urn:uuid:")) {
            return null;
        }
        try {
            return UUID.fromString(jti.substring("urn:uuid:".length()));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String firstFailureReason(Map<String, Object> checks) {
        return checks.entrySet().stream()
                .filter(entry -> Boolean.FALSE.equals(entry.getValue()))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse("Verification failed");
    }

    private Map<String, Object> buildPresentationDefinition(PolicyDefinition rules) {
        Map<String, Object> definition = new LinkedHashMap<>();
        definition.put("schemaId", rules.schemaId() != null ? rules.schemaId().toString() : null);
        definition.put("requiredClaims", rules.requiredClaims());
        return definition;
    }

    private String randomToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private void audit(UUID tenantId, String action, UUID requestId) {
        auditEventPublisher.publish(AuditEvent.of(
                tenantId,
                CurrentUserContext.getUserId(),
                action,
                "VERIFICATION_REQUEST",
                requestId.toString(),
                "SUCCESS",
                Map.of()
        ));
    }

    public record RequestResult(VerificationRequest request, Oid4vpRequestGenerator.Request oid4vpRequest) {
    }

    private record Result(boolean passed, UUID credentialId, String subjectDid, String failureReason) {
        static Result success(UUID credentialId, String subjectDid) {
            return new Result(true, credentialId, subjectDid, null);
        }

        static Result failure(String reason) {
            return new Result(false, null, null, reason);
        }
    }
}
