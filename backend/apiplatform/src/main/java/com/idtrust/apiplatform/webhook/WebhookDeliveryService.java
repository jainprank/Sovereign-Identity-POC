package com.idtrust.apiplatform.webhook;

import com.idtrust.common.web.ApiErrorCode;
import com.idtrust.common.web.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Implements FR-API-009 (Webhook Retry): visibility and manual remediation over previously
 * recorded {@link WebhookDelivery}s. Mirrors {@code hedera.HederaAnchorService}'s
 * retry-only-from-FAILED guard exactly - there is no background auto-retry/backoff daemon, since
 * no scheduling infrastructure exists anywhere else in this codebase either.
 */
@Service
public class WebhookDeliveryService {

    private final WebhookDeliveryRepository webhookDeliveryRepository;
    private final WebhookEndpointRepository webhookEndpointRepository;
    private final WebhookEventPublisherImpl webhookEventPublisher;

    public WebhookDeliveryService(WebhookDeliveryRepository webhookDeliveryRepository,
                                   WebhookEndpointRepository webhookEndpointRepository,
                                   WebhookEventPublisherImpl webhookEventPublisher) {
        this.webhookDeliveryRepository = webhookDeliveryRepository;
        this.webhookEndpointRepository = webhookEndpointRepository;
        this.webhookEventPublisher = webhookEventPublisher;
    }

    public WebhookDelivery get(UUID tenantId, UUID id) {
        return webhookDeliveryRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new ApiException(ApiErrorCode.RESOURCE_NOT_FOUND, "Webhook delivery not found: " + id));
    }

    public List<WebhookDelivery> listForEndpoint(UUID tenantId, UUID webhookEndpointId) {
        return webhookDeliveryRepository.findByTenantIdAndWebhookEndpointId(tenantId, webhookEndpointId);
    }

    @Transactional
    public WebhookDelivery retry(UUID tenantId, UUID id) {
        WebhookDelivery delivery = get(tenantId, id);
        if (delivery.getStatus() != WebhookDeliveryStatus.FAILED) {
            throw new ApiException(ApiErrorCode.INVALID_REQUEST, "Only a FAILED webhook delivery can be retried: " + id);
        }
        WebhookEndpoint endpoint = webhookEndpointRepository.findById(delivery.getWebhookEndpointId())
                .orElseThrow(() -> new ApiException(ApiErrorCode.RESOURCE_NOT_FOUND,
                        "Webhook endpoint no longer exists: " + delivery.getWebhookEndpointId()));
        webhookEventPublisher.deliver(endpoint, delivery, delivery.getPayload());
        webhookDeliveryRepository.save(delivery);
        return delivery;
    }
}
