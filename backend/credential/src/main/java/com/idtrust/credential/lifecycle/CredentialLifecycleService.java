package com.idtrust.credential.lifecycle;

import com.idtrust.common.audit.AuditEvent;
import com.idtrust.common.audit.AuditEventPublisher;
import com.idtrust.common.tenant.CurrentUserContext;
import com.idtrust.common.web.ApiErrorCode;
import com.idtrust.common.web.ApiException;
import com.idtrust.credential.issuance.Credential;
import com.idtrust.credential.issuance.CredentialIssuanceService;
import com.idtrust.credential.issuance.CredentialRepository;
import com.idtrust.credential.issuance.CredentialStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Implements EPIC 7 (Credential Lifecycle): FR-CRED-004 through FR-CRED-011, FR-CRED-013,
 * FR-CRED-014. FR-CRED-001 (Credential Registry), FR-CRED-002 (Credential Search) and FR-CRED-003
 * (Credential Detail) are already satisfied by {@link CredentialIssuanceService#search} and
 * {@link CredentialIssuanceService#get}, built in E07 - no separate registry resource is needed.
 * FR-CRED-012 (Bitstring Status List) is V1-priority and out of scope: {@link #statusList} instead
 * returns a plain id/status list (FR-CRED-011), not a W3C StatusList2021 bitstring credential.
 *
 * <p>Renewal vs. reissuance is a deliberate distinction, since the spec's acceptance criteria for
 * both are the same generic template: {@link #renew} mints a fresh credential with the <em>same</em>
 * claims and a new expiry, marking the original {@code EXPIRED} (superseded, no wrongdoing
 * implied); {@link #reissue} mints a fresh credential with <em>corrected</em> claims, marking the
 * original {@code REVOKED} (its data is now considered incorrect, so it must not remain usable).
 */
@Service
public class CredentialLifecycleService {

    private final CredentialRepository credentialRepository;
    private final CredentialStatusEventRepository statusEventRepository;
    private final CredentialIssuanceService credentialIssuanceService;
    private final AuditEventPublisher auditEventPublisher;

    public CredentialLifecycleService(CredentialRepository credentialRepository,
                                       CredentialStatusEventRepository statusEventRepository,
                                       CredentialIssuanceService credentialIssuanceService,
                                       AuditEventPublisher auditEventPublisher) {
        this.credentialRepository = credentialRepository;
        this.statusEventRepository = statusEventRepository;
        this.credentialIssuanceService = credentialIssuanceService;
        this.auditEventPublisher = auditEventPublisher;
    }

    @Transactional
    public Credential revoke(UUID tenantId, UUID id, String reason) {
        Credential credential = credentialIssuanceService.get(tenantId, id);
        if (credential.getStatus() == CredentialStatus.REVOKED) {
            throw new ApiException(ApiErrorCode.INVALID_REQUEST, "Credential is already revoked: " + id);
        }
        if (credential.getStatus() == CredentialStatus.EXPIRED) {
            throw new ApiException(ApiErrorCode.INVALID_REQUEST, "An expired credential cannot be revoked: " + id);
        }
        transition(tenantId, credential, CredentialStatus.REVOKED, reason, credential::revoke, "CREDENTIAL_REVOKED");
        credentialIssuanceService.publishWebhookEvent(credential, "CredentialRevoked");
        return credential;
    }

    @Transactional
    public Credential suspend(UUID tenantId, UUID id, String reason) {
        Credential credential = requireStatus(tenantId, id, CredentialStatus.ISSUED,
                "Only an ISSUED credential can be suspended");
        transition(tenantId, credential, CredentialStatus.SUSPENDED, reason, credential::suspend, "CREDENTIAL_SUSPENDED");
        credentialIssuanceService.publishWebhookEvent(credential, "CredentialSuspended");
        return credential;
    }

    @Transactional
    public Credential reinstate(UUID tenantId, UUID id) {
        Credential credential = requireStatus(tenantId, id, CredentialStatus.SUSPENDED,
                "Only a SUSPENDED credential can be reinstated");
        transition(tenantId, credential, CredentialStatus.ISSUED, null, reason -> credential.reinstate(), "CREDENTIAL_REINSTATED");
        return credential;
    }

    @Transactional
    public CredentialIssuanceService.IssuedCredential renew(UUID tenantId, UUID id, Instant newExpiresAt) {
        Credential credential = requireStatus(tenantId, id, CredentialStatus.ISSUED,
                "Only an ISSUED credential can be renewed");
        CredentialIssuanceService.IssuedCredential renewed = credentialIssuanceService.issue(tenantId,
                credential.getSchemaId(), credential.getIssuerDid(), credential.getSubjectDid(),
                credential.getClaims(), newExpiresAt);
        transition(tenantId, credential, CredentialStatus.EXPIRED,
                "Superseded by renewal " + renewed.credential().getId(), reason -> credential.expire(reason),
                "CREDENTIAL_RENEWED");
        return renewed;
    }

    @Transactional
    public CredentialIssuanceService.IssuedCredential reissue(UUID tenantId, UUID id, Map<String, Object> newClaims,
                                                                Instant newExpiresAt) {
        Credential credential = credentialIssuanceService.get(tenantId, id);
        if (credential.getStatus() == CredentialStatus.REVOKED) {
            throw new ApiException(ApiErrorCode.INVALID_REQUEST,
                    "A revoked credential cannot be reissued; issue a new credential instead: " + id);
        }
        Map<String, Object> claims = newClaims != null ? newClaims : credential.getClaims();
        Instant expiresAt = newExpiresAt != null ? newExpiresAt : credential.getExpiresAt();
        CredentialIssuanceService.IssuedCredential reissued = credentialIssuanceService.issue(tenantId,
                credential.getSchemaId(), credential.getIssuerDid(), credential.getSubjectDid(), claims, expiresAt);
        transition(tenantId, credential, CredentialStatus.REVOKED,
                "Superseded by reissuance " + reissued.credential().getId(), reason -> credential.revoke(reason),
                "CREDENTIAL_REISSUED");
        return reissued;
    }

    /** FR-CRED-014 - the current status after lazily applying FR-CRED-008 expiration. */
    @Transactional
    public CredentialStatus checkStatus(UUID tenantId, UUID id) {
        return effectiveStatus(tenantId, credentialIssuanceService.get(tenantId, id));
    }

    /** FR-CRED-011 - a plain id/status list; see class Javadoc for why not a bitstring (FR-CRED-012). */
    @Transactional
    public List<StatusListEntry> statusList(UUID tenantId, UUID schemaId) {
        List<Credential> credentials = schemaId != null
                ? credentialRepository.findByTenantIdAndSchemaId(tenantId, schemaId)
                : credentialRepository.findByTenantId(tenantId);
        return credentials.stream()
                .map(credential -> new StatusListEntry(credential.getId(), effectiveStatus(tenantId, credential)))
                .toList();
    }

    public List<CredentialStatusEvent> history(UUID tenantId, UUID credentialId) {
        credentialIssuanceService.get(tenantId, credentialId);
        return statusEventRepository.findByTenantIdAndCredentialIdOrderByCreatedAtDesc(tenantId, credentialId);
    }

    /**
     * FR-CRED-008 - applied lazily on read (no background sweep exists in this codebase yet; the
     * only scheduled/async infrastructure specified anywhere, Kafka, hasn't been wired up by any
     * epic so far, so building a dedicated expiration worker now would be premature).
     */
    private CredentialStatus effectiveStatus(UUID tenantId, Credential credential) {
        if (credential.getStatus() == CredentialStatus.ISSUED
                && credential.getExpiresAt() != null
                && credential.getExpiresAt().isBefore(Instant.now())) {
            transition(tenantId, credential, CredentialStatus.EXPIRED, "Expired at " + credential.getExpiresAt(),
                    reason -> credential.expire(reason), "CREDENTIAL_EXPIRED");
        }
        return credential.getStatus();
    }

    private Credential requireStatus(UUID tenantId, UUID id, CredentialStatus required, String message) {
        Credential credential = credentialIssuanceService.get(tenantId, id);
        if (credential.getStatus() != required) {
            throw new ApiException(ApiErrorCode.INVALID_REQUEST, message + ": " + id);
        }
        return credential;
    }

    private void transition(UUID tenantId, Credential credential, CredentialStatus newStatus, String reason,
                             Consumer<String> mutator, String auditAction) {
        CredentialStatus previousStatus = credential.getStatus();
        mutator.accept(reason);
        credentialRepository.save(credential);
        statusEventRepository.save(new CredentialStatusEvent(tenantId, credential.getId(), previousStatus, newStatus, reason));
        audit(tenantId, auditAction, credential.getId());
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

    public record StatusListEntry(UUID credentialId, CredentialStatus status) {
    }
}
