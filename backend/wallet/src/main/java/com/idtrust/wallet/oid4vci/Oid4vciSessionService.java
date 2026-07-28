package com.idtrust.wallet.oid4vci;

import com.idtrust.common.audit.AuditEvent;
import com.idtrust.common.audit.AuditEventPublisher;
import com.idtrust.common.tenant.CurrentUserContext;
import com.idtrust.common.web.ApiErrorCode;
import com.idtrust.common.web.ApiException;
import com.idtrust.credential.issuance.Credential;
import com.idtrust.credential.issuance.CredentialIssuanceService;
import com.idtrust.credential.schema.CredentialSchema;
import com.idtrust.credential.schema.CredentialSchemaService;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

/**
 * Implements EPIC 9 (OID4VCI) for the pre-authorized-code grant: FR-OID4VCI-003/004/005
 * (Credential/QR/Deep Link Offer, via {@link Oid4vciOfferGenerator}), FR-OID4VCI-006 (Pre-
 * Authorized Code), FR-OID4VCI-009 (Token Endpoint), FR-OID4VCI-010 (Credential Endpoint),
 * FR-OID4VCI-011 (Access Token Validation), FR-OID4VCI-012 (Credential Response), FR-OID4VCI-014
 * (Issuance Session). FR-OID4VCI-013 (Error Response) needs no dedicated code here - every failure
 * below reuses the platform-wide {@link ApiException}/{@link ApiErrorCode} catalogue.
 *
 * <p>FR-OID4VCI-007/008 (Authorization Code Flow, Authorization Endpoint) are out of scope for
 * this pass: that flow requires an interactive, browser-redirect-based user authorization step,
 * and no wallet UI exists yet to drive it (E15). The pre-authorized-code grant alone is a complete
 * MVP path here, since this platform's own credential issuance (E07) is already issuer-initiated
 * and synchronous - the holder never needs to "authorize" access to claims they didn't choose.
 * FR-OID4VCI-015 (Wallet Notification) is V1-priority and also out of scope - no notification
 * infrastructure (EPIC 17) exists yet.
 *
 * <p>FR-OID4VCI-001/002 (Issuer Metadata, Credential Configuration) are implemented by
 * {@link #issuerMetadata}, derived from the tenant's PUBLISHED {@code CredentialSchema}s rather
 * than a separate configuration resource - consistent with schemas already being this platform's
 * only notion of a "credential configuration".
 */
@Service
public class Oid4vciSessionService {

    private static final Duration SESSION_TTL = Duration.ofMinutes(10);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final Oid4vciSessionRepository sessionRepository;
    private final CredentialIssuanceService credentialIssuanceService;
    private final CredentialSchemaService credentialSchemaService;
    private final AuditEventPublisher auditEventPublisher;
    private final MeterRegistry meterRegistry;

    public Oid4vciSessionService(Oid4vciSessionRepository sessionRepository,
                                  CredentialIssuanceService credentialIssuanceService,
                                  CredentialSchemaService credentialSchemaService,
                                  AuditEventPublisher auditEventPublisher, MeterRegistry meterRegistry) {
        this.sessionRepository = sessionRepository;
        this.credentialIssuanceService = credentialIssuanceService;
        this.credentialSchemaService = credentialSchemaService;
        this.auditEventPublisher = auditEventPublisher;
        this.meterRegistry = meterRegistry;
    }

    @Transactional
    public OfferResult createOffer(UUID tenantId, UUID credentialId) {
        Credential credential = credentialIssuanceService.get(tenantId, credentialId);
        CredentialSchema schema = credentialSchemaService.get(tenantId, credential.getSchemaId());

        String preAuthorizedCode = randomToken();
        Instant expiresAt = Instant.now().plus(SESSION_TTL);
        Oid4vciSession session = new Oid4vciSession(tenantId, credentialId, preAuthorizedCode, expiresAt);
        sessionRepository.save(session);

        Oid4vciOfferGenerator.Offer offer = Oid4vciOfferGenerator.generate(
                credential.getIssuerDid(), schema.getName(), preAuthorizedCode);
        audit(tenantId, "OID4VCI_OFFER_CREATED", session.getId());
        return new OfferResult(session, offer);
    }

    @Transactional
    public TokenResult issueToken(UUID tenantId, String preAuthorizedCode) {
        Oid4vciSession session = sessionRepository.findByTenantIdAndPreAuthorizedCode(tenantId, preAuthorizedCode)
                .orElseThrow(() -> new ApiException(ApiErrorCode.INVALID_REQUEST, "Invalid pre-authorized code"));
        requireLiveStatus(session, Oid4vciSessionStatus.OFFERED, "Pre-authorized code has already been used or is invalid");

        String accessToken = randomToken();
        session.issueToken(accessToken);
        audit(tenantId, "OID4VCI_TOKEN_ISSUED", session.getId());
        return new TokenResult(accessToken, Duration.between(Instant.now(), session.getExpiresAt()).getSeconds());
    }

    @Transactional
    public CredentialResult redeemCredential(UUID tenantId, String accessToken) {
        Oid4vciSession session = sessionRepository.findByTenantIdAndAccessToken(tenantId, accessToken)
                .orElseThrow(() -> new ApiException(ApiErrorCode.UNAUTHORIZED, "Invalid access token"));
        requireLiveStatus(session, Oid4vciSessionStatus.TOKEN_ISSUED, "Access token has already been used or is invalid");

        Credential credential = credentialIssuanceService.get(tenantId, session.getCredentialId());
        session.claim();
        audit(tenantId, "OID4VCI_CREDENTIAL_CLAIMED", session.getId());
        // FR-ANL-004 (Wallet Metrics) - a claimed credential is this platform's one genuine
        // "wallet activity" signal; no wallet UI/session-analytics event exists beyond this.
        meterRegistry.counter("idtrust.wallet.credentials_claimed").increment();
        return new CredentialResult(credential);
    }

    public Map<String, Object> issuerMetadata(UUID tenantId) {
        Map<String, Object> configurations = credentialSchemaService
                .search(tenantId, null, com.idtrust.credential.schema.SchemaStatus.PUBLISHED).stream()
                .collect(java.util.stream.Collectors.toMap(CredentialSchema::getName, schema -> Map.of(
                        "format", schema.getFormat().name(),
                        "claims", schema.getDefinition().claims().stream()
                                .map(claim -> Map.of("name", claim.name(), "type", claim.type().name(),
                                        "required", claim.required()))
                                .toList()
                )));
        return Map.of(
                "credential_issuer", "urn:idtrust:tenant:" + tenantId,
                "credential_endpoint", "/api/v1/oid4vci/credential",
                "token_endpoint", "/api/v1/oid4vci/token",
                "credential_configurations_supported", configurations
        );
    }

    private void requireLiveStatus(Oid4vciSession session, Oid4vciSessionStatus required, String invalidMessage) {
        if (session.isExpired()) {
            session.expire();
            throw new ApiException(ApiErrorCode.INVALID_REQUEST, "This OID4VCI session has expired");
        }
        if (session.getStatus() != required) {
            throw new ApiException(ApiErrorCode.INVALID_REQUEST, invalidMessage);
        }
    }

    private String randomToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private void audit(UUID tenantId, String action, UUID sessionId) {
        auditEventPublisher.publish(AuditEvent.of(
                tenantId,
                CurrentUserContext.getUserId(),
                action,
                "OID4VCI_SESSION",
                sessionId.toString(),
                "SUCCESS",
                Map.of()
        ));
    }

    public record OfferResult(Oid4vciSession session, Oid4vciOfferGenerator.Offer offer) {
    }

    public record TokenResult(String accessToken, long expiresInSeconds) {
    }

    public record CredentialResult(Credential credential) {
    }
}
