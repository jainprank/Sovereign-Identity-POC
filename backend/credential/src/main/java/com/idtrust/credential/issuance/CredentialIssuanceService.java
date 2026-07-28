package com.idtrust.credential.issuance;

import com.idtrust.common.audit.AuditEvent;
import com.idtrust.common.audit.AuditEventPublisher;
import com.idtrust.common.hedera.HederaAnchorPublisher;
import com.idtrust.common.hedera.HederaAnchorRequest;
import com.idtrust.common.tenant.CurrentUserContext;
import com.idtrust.common.web.ApiErrorCode;
import com.idtrust.common.web.ApiException;
import com.idtrust.credential.issuance.sdjwt.SdJwtVcSigner;
import com.idtrust.credential.schema.CredentialFormat;
import com.idtrust.credential.schema.CredentialSchema;
import com.idtrust.credential.schema.CredentialSchemaService;
import com.idtrust.credential.schema.SchemaStatus;
import com.idtrust.did.Did;
import com.idtrust.did.DidService;
import com.idtrust.kms.key.KeyService;
import com.idtrust.kms.key.ManagedKey;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Implements EPIC 6 (Credential Issuance): FR-ISS-001 through FR-ISS-013 and FR-ISS-020. Scope
 * boundaries, decided against the Baseline's literal API example (a synchronous
 * {@code POST /api/v1/credentials} that returns {@code "status": "ISSUED"} immediately, with no
 * pending/claimable offer state):
 * <ul>
 *   <li>FR-ISS-001 (Create Credential Definition) and FR-ISS-002 (Select Schema) are folded into a
 *       single issuance request rather than a separate "credential definition" resource - see
 *       {@link #issue}.</li>
 *   <li>FR-ISS-014 (Cancel Issuance) and FR-ISS-015 (Retry Failed Issuance) are deliberately out of
 *       scope for this pass: cancel/retry imply an async offer/claim state machine that isn't
 *       specified anywhere and properly belongs with E09's OID4VCI wallet-claim flow (not yet
 *       built) - inventing one now would be speculative, mirroring E06's precedent of explicitly
 *       skipping FR-SCH-012/013.</li>
 *   <li>FR-ISS-016 through FR-ISS-019 (Bulk Issuance, Dynamic Claim Population, External Data
 *       Source, Credential Preview) are V1-priority and out of scope.</li>
 * </ul>
 */
@Service
public class CredentialIssuanceService {

    private final CredentialRepository credentialRepository;
    private final CredentialSchemaService schemaService;
    private final DidService didService;
    private final KeyService keyService;
    private final AuditEventPublisher auditEventPublisher;
    private final HederaAnchorPublisher hederaAnchorPublisher;

    public CredentialIssuanceService(CredentialRepository credentialRepository, CredentialSchemaService schemaService,
                                      DidService didService, KeyService keyService,
                                      AuditEventPublisher auditEventPublisher, HederaAnchorPublisher hederaAnchorPublisher) {
        this.credentialRepository = credentialRepository;
        this.schemaService = schemaService;
        this.didService = didService;
        this.keyService = keyService;
        this.auditEventPublisher = auditEventPublisher;
        this.hederaAnchorPublisher = hederaAnchorPublisher;
    }

    @Transactional
    public IssuedCredential issue(UUID tenantId, UUID schemaId, String issuerDid, String subjectDid,
                                   Map<String, Object> claims, Instant expiresAt) {
        CredentialSchema schema = schemaService.get(tenantId, schemaId);
        if (schema.getStatus() != SchemaStatus.PUBLISHED) {
            throw new ApiException(ApiErrorCode.INVALID_REQUEST,
                    "Only a PUBLISHED schema can be used for issuance: " + schemaId);
        }
        ClaimValidator.validate(schema.getDefinition(), claims);

        Did issuer = didService.getByDidString(tenantId, issuerDid);
        if (subjectDid != null) {
            didService.resolve(subjectDid);
        }
        ManagedKey signingKey = keyService.getActiveKeyForDid(issuer.getId());

        UUID credentialId = UUID.randomUUID();
        String credentialJwt;
        String credentialHash;
        if (schema.getFormat() == CredentialFormat.SD_JWT_VC) {
            SdJwtVcSigner.SignedSdJwt signed = SdJwtVcSigner.sign(
                    tenantId, credentialId, issuer, subjectDid, schema.getName(), claims, expiresAt, signingKey, keyService);
            credentialJwt = signed.combined();
            credentialHash = signed.credentialHash();
        } else {
            JwtVerifiableCredentialSigner.SignedCredential signed = JwtVerifiableCredentialSigner.sign(
                    tenantId, credentialId, issuer, subjectDid, schema.getName(), claims, expiresAt, signingKey, keyService);
            credentialJwt = signed.jwt();
            credentialHash = signed.credentialHash();
        }

        Credential credential = new Credential(credentialId, tenantId, schemaId, issuerDid, subjectDid,
                schema.getFormat(), credentialJwt, credentialHash, claims, expiresAt);
        credentialRepository.save(credential);
        audit(tenantId, "CREDENTIAL_ISSUED", credential.getId());
        anchorCredential(credential);

        CredentialOfferGenerator.CredentialOffer offer = CredentialOfferGenerator.generate(issuerDid, credentialId);
        return new IssuedCredential(credential, offer);
    }

    public Credential get(UUID tenantId, UUID id) {
        return credentialRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new ApiException(ApiErrorCode.RESOURCE_NOT_FOUND, "Credential not found: " + id));
    }

    public List<Credential> search(UUID tenantId, String subjectDid, CredentialStatus status) {
        return credentialRepository.search(tenantId, subjectDid, status);
    }

    private void audit(UUID tenantId, String action, UUID credentialId) {
        auditEventPublisher.publish(AuditEvent.of(
                tenantId,
                CurrentUserContext.getUserId(),
                action,
                "CREDENTIAL",
                credentialId.toString(),
                "SUCCESS",
                Map.of()
        ));
    }

    /**
     * FR-HED-007 (Credential Integrity Hash) - reuses the already-computed {@code credentialHash}
     * (SHA-256 of the signed JWT, computed by {@code JwtVerifiableCredentialSigner}/{@code
     * SdJwtVcSigner} at signing time) rather than hashing again, via {@link
     * HederaAnchorRequest#ofHash}. FR-HED-008 (Timestamp Anchoring) needs no separate
     * implementation - it is satisfied by the {@code consensusTimestamp} Hedera assigns this anchor.
     */
    private void anchorCredential(Credential credential) {
        try {
            hederaAnchorPublisher.anchor(HederaAnchorRequest.ofHash(
                    credential.getTenantId(), "CREDENTIAL_ANCHORED", "CREDENTIAL", credential.getId(),
                    credential.getCredentialHash()));
        } catch (Exception e) {
            // Never let an anchoring failure affect credential issuance itself.
        }
    }

    /** Pairs a newly issued credential with its one-time credential offer (FR-ISS-009/010/011). */
    public record IssuedCredential(Credential credential, CredentialOfferGenerator.CredentialOffer offer) {
    }
}
