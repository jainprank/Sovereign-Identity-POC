package com.idtrust.did;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.idtrust.common.audit.AuditEvent;
import com.idtrust.common.audit.AuditEventPublisher;
import com.idtrust.common.hedera.HederaAnchorPublisher;
import com.idtrust.common.hedera.HederaAnchorRequest;
import com.idtrust.common.tenant.CurrentUserContext;
import com.idtrust.common.web.ApiErrorCode;
import com.idtrust.common.web.ApiException;
import com.idtrust.common.webhook.WebhookEvent;
import com.idtrust.common.webhook.WebhookEventPublisher;
import com.idtrust.did.provider.DidCreationRequest;
import com.idtrust.did.provider.DidMethod;
import com.idtrust.did.provider.DidMethodProvider;
import com.idtrust.did.provider.DidMethodProviderRegistry;
import com.idtrust.did.provider.DidProviderResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Implements FR-DID-* DID lifecycle operations (create, resolve, update/rotate, deactivate),
 * delegating method-specific behavior to {@link DidMethodProvider}. Controller-reachable
 * operations take an explicit {@code tenantId}, matching this codebase's established tenant-
 * scoping convention (see {@code identity.user.UserService}); {@link #resolve} is the exception -
 * DID resolution is a public, cross-tenant operation with no ownership check, mirroring how any
 * DID resolver on the internet can resolve any DID.
 */
@Service
public class DidService {

    private final DidRepository didRepository;
    private final DidMethodProviderRegistry providerRegistry;
    private final AuditEventPublisher auditEventPublisher;
    private final HederaAnchorPublisher hederaAnchorPublisher;
    private final WebhookEventPublisher webhookEventPublisher;
    private final ObjectMapper objectMapper;

    public DidService(DidRepository didRepository, DidMethodProviderRegistry providerRegistry,
                       AuditEventPublisher auditEventPublisher, HederaAnchorPublisher hederaAnchorPublisher,
                       WebhookEventPublisher webhookEventPublisher, ObjectMapper objectMapper) {
        this.didRepository = didRepository;
        this.providerRegistry = providerRegistry;
        this.auditEventPublisher = auditEventPublisher;
        this.hederaAnchorPublisher = hederaAnchorPublisher;
        this.webhookEventPublisher = webhookEventPublisher;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Did create(UUID tenantId, DidMethod method, String domain, String path) {
        UUID id = UUID.randomUUID();
        DidMethodProvider provider = providerRegistry.get(method);
        DidProviderResult result = provider.create(new DidCreationRequest(tenantId, id, domain, path));

        Did did = new Did(id, tenantId, method, result.did(), result.document());
        didRepository.save(did);
        audit(tenantId, "DID_CREATED", did.getId());
        anchorDocument(did);
        publishWebhookEvent(did, "DIDCreated");
        return did;
    }

    public Did get(UUID tenantId, UUID id) {
        return didRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new ApiException(ApiErrorCode.RESOURCE_NOT_FOUND, "DID not found: " + id));
    }

    public List<Did> listByTenant(UUID tenantId) {
        return didRepository.findByTenantId(tenantId);
    }

    public Page<Did> listByTenant(UUID tenantId, Pageable pageable) {
        return didRepository.findByTenantId(tenantId, pageable);
    }

    /**
     * Looks up a DID by its string identifier, scoped to a tenant - used by
     * {@code com.idtrust.credential.issuance.CredentialIssuanceService} to verify that an issuer
     * DID (FR-ISS-005) is actually owned by the requesting tenant, unlike {@link #resolve} which
     * deliberately performs no ownership check.
     */
    public Did getByDidString(UUID tenantId, String did) {
        return didRepository.findByTenantIdAndDid(tenantId, did)
                .orElseThrow(() -> new ApiException(ApiErrorCode.RESOURCE_NOT_FOUND,
                        "DID not found for this tenant: " + did));
    }

    /** Resolves any DID (not necessarily one this platform manages) - see class Javadoc. */
    public Map<String, Object> resolve(String did) {
        DidMethod method = methodFromIdentifier(did);
        return providerRegistry.get(method).resolve(did);
    }

    @Transactional
    public Did rotate(UUID tenantId, UUID id) {
        Did did = get(tenantId, id);
        if (did.getStatus() != DidStatus.ACTIVE) {
            throw new ApiException(ApiErrorCode.INVALID_REQUEST, "Only an active DID can be rotated: " + id);
        }
        DidProviderResult result = providerRegistry.get(did.getMethod()).update(did);
        did.replaceDocument(result.document());
        audit(tenantId, "DID_KEY_ROTATED", did.getId());
        publishWebhookEvent(did, "DIDUpdated");
        return did;
    }

    @Transactional
    public Did deactivate(UUID tenantId, UUID id) {
        Did did = get(tenantId, id);
        providerRegistry.get(did.getMethod()).deactivate(did);
        did.deactivate();
        audit(tenantId, "DID_DEACTIVATED", did.getId());
        publishWebhookEvent(did, "DIDUpdated");
        return did;
    }

    private DidMethod methodFromIdentifier(String did) {
        if (did.startsWith("did:key:")) {
            return DidMethod.KEY;
        }
        if (did.startsWith("did:web:")) {
            return DidMethod.WEB;
        }
        throw new ApiException(ApiErrorCode.INVALID_DID, "Unsupported or malformed DID: " + did);
    }

    private void audit(UUID tenantId, String action, UUID didId) {
        auditEventPublisher.publish(AuditEvent.of(
                tenantId,
                CurrentUserContext.getUserId(),
                action,
                "DID",
                didId.toString(),
                "SUCCESS",
                Map.of()
        ));
    }

    /** FR-HED-005 (DID Anchoring) - hashes the DID Document itself, never a private key or raw claim. */
    private void anchorDocument(Did did) {
        try {
            String content = objectMapper.writeValueAsString(did.getDocument());
            hederaAnchorPublisher.anchor(HederaAnchorRequest.ofRawContent(
                    did.getTenantId(), "DID_ANCHORED", "DID", did.getId(), content));
        } catch (Exception e) {
            // Never let an anchoring failure affect DID creation itself.
        }
    }

    /** EPIC 14 (FR-API-008 Webhooks) - relays a DID lifecycle change to this tenant's subscribed webhooks. */
    private void publishWebhookEvent(Did did, String eventType) {
        try {
            webhookEventPublisher.publish(WebhookEvent.of(did.getTenantId(), eventType, "DID", did.getId(),
                    Map.of("did", did.getDid())));
        } catch (Exception e) {
            // Never let a webhook delivery failure affect the DID operation itself.
        }
    }
}
