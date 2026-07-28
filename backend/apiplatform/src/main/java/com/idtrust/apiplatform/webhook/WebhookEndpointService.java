package com.idtrust.apiplatform.webhook;

import com.idtrust.apiplatform.auth.CredentialHasher;
import com.idtrust.common.audit.AuditEvent;
import com.idtrust.common.audit.AuditEventPublisher;
import com.idtrust.common.tenant.CurrentUserContext;
import com.idtrust.common.web.ApiErrorCode;
import com.idtrust.common.web.ApiException;
import com.idtrust.kms.crypto.KeyEncryptionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Implements the management half of FR-API-008 (Webhooks): CRUD over {@link WebhookEndpoint}s. */
@Service
public class WebhookEndpointService {

    private final WebhookEndpointRepository webhookEndpointRepository;
    private final KeyEncryptionService keyEncryptionService;
    private final AuditEventPublisher auditEventPublisher;

    public WebhookEndpointService(WebhookEndpointRepository webhookEndpointRepository,
                                   KeyEncryptionService keyEncryptionService, AuditEventPublisher auditEventPublisher) {
        this.webhookEndpointRepository = webhookEndpointRepository;
        this.keyEncryptionService = keyEncryptionService;
        this.auditEventPublisher = auditEventPublisher;
    }

    /** Returns the endpoint alongside its one-time plaintext secret - see {@link WebhookEndpoint}'s Javadoc. */
    @Transactional
    public WebhookEndpointCreationResult create(UUID tenantId, String url, List<String> eventTypes) {
        WebhookEventTypes.validate(eventTypes);
        String secret = CredentialHasher.generateSecret();
        WebhookEndpoint endpoint = new WebhookEndpoint(tenantId, url, eventTypes,
                keyEncryptionService.encrypt(secret.getBytes(StandardCharsets.UTF_8)));
        webhookEndpointRepository.save(endpoint);
        audit(tenantId, "WEBHOOK_ENDPOINT_CREATED", endpoint.getId());
        return new WebhookEndpointCreationResult(endpoint, secret);
    }

    public WebhookEndpoint get(UUID tenantId, UUID id) {
        return webhookEndpointRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new ApiException(ApiErrorCode.RESOURCE_NOT_FOUND, "Webhook endpoint not found: " + id));
    }

    public List<WebhookEndpoint> list(UUID tenantId) {
        return webhookEndpointRepository.findByTenantId(tenantId);
    }

    @Transactional
    public WebhookEndpoint deactivate(UUID tenantId, UUID id) {
        WebhookEndpoint endpoint = get(tenantId, id);
        endpoint.deactivate();
        audit(tenantId, "WEBHOOK_ENDPOINT_DEACTIVATED", endpoint.getId());
        return endpoint;
    }

    @Transactional
    public WebhookEndpoint activate(UUID tenantId, UUID id) {
        WebhookEndpoint endpoint = get(tenantId, id);
        endpoint.activate();
        audit(tenantId, "WEBHOOK_ENDPOINT_ACTIVATED", endpoint.getId());
        return endpoint;
    }

    private void audit(UUID tenantId, String action, UUID endpointId) {
        auditEventPublisher.publish(AuditEvent.of(
                tenantId, CurrentUserContext.getUserId(), action, "WEBHOOK_ENDPOINT", endpointId.toString(), "SUCCESS", Map.of()));
    }

    public record WebhookEndpointCreationResult(WebhookEndpoint endpoint, String secret) {
    }
}
