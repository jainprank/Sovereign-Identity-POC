package com.idtrust.apiplatform.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.idtrust.common.webhook.WebhookEvent;
import com.idtrust.common.webhook.WebhookEventPublisher;
import com.idtrust.kms.crypto.KeyEncryptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * The single real {@link WebhookEventPublisher} implementation (FR-API-008 Webhooks). For every
 * {@link WebhookEndpoint} of the event's tenant that is {@code active} and subscribed to
 * {@code event.eventType()}, performs a genuine HTTP POST (JDK {@code HttpClient}, mirroring
 * {@code did.provider.didweb.DidWebProvider}'s live-HTTP precedent) with an HMAC-SHA256-signed
 * body, and persists a {@link WebhookDelivery} row recording the outcome either way.
 *
 * <p>Never throws - a delivery failure (network error, non-2xx response, or even a failure to look
 * up subscribed endpoints) is recorded but must never affect the domain operation that triggered
 * this event, exactly like {@code HederaAnchorPublisher}'s contract.
 */
@Component
public class WebhookEventPublisherImpl implements WebhookEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(WebhookEventPublisherImpl.class);
    private static final Duration DELIVERY_TIMEOUT = Duration.ofSeconds(5);
    private static final String SIGNATURE_HEADER = "X-IDTrust-Signature";

    private final WebhookEndpointRepository webhookEndpointRepository;
    private final WebhookDeliveryRepository webhookDeliveryRepository;
    private final KeyEncryptionService keyEncryptionService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(DELIVERY_TIMEOUT).build();

    public WebhookEventPublisherImpl(WebhookEndpointRepository webhookEndpointRepository,
                                      WebhookDeliveryRepository webhookDeliveryRepository,
                                      KeyEncryptionService keyEncryptionService, ObjectMapper objectMapper) {
        this.webhookEndpointRepository = webhookEndpointRepository;
        this.webhookDeliveryRepository = webhookDeliveryRepository;
        this.keyEncryptionService = keyEncryptionService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void publish(WebhookEvent event) {
        try {
            List<WebhookEndpoint> subscribers = webhookEndpointRepository.findByTenantIdAndActiveTrue(event.tenantId())
                    .stream()
                    .filter(endpoint -> endpoint.subscribesTo(event.eventType()))
                    .toList();
            String body = objectMapper.writeValueAsString(Map.of(
                    "eventType", event.eventType(),
                    "resourceType", event.resourceType(),
                    "resourceId", event.resourceId(),
                    "occurredAt", event.occurredAt().toString(),
                    "payload", event.payload() == null ? Map.of() : event.payload()));

            for (WebhookEndpoint endpoint : subscribers) {
                WebhookDelivery delivery = new WebhookDelivery(event.tenantId(), endpoint.getId(), event.eventType(),
                        event.resourceType(), event.resourceId(), body);
                deliver(endpoint, delivery, body);
                webhookDeliveryRepository.save(delivery);
            }
        } catch (Exception e) {
            log.warn("Failed to publish webhook event {} for tenant {}: {}", event.eventType(), event.tenantId(),
                    e.getMessage());
        }
    }

    /** FR-API-009 (Webhook Retry) - re-attempts a single {@link WebhookDelivery} against its endpoint, in place. */
    void deliver(WebhookEndpoint endpoint, WebhookDelivery delivery, String body) {
        try {
            String secret = new String(keyEncryptionService.decrypt(endpoint.getEncryptedSecret()), StandardCharsets.UTF_8);
            String signature = WebhookSigner.sign(secret, body);
            HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint.getUrl()))
                    .timeout(DELIVERY_TIMEOUT)
                    .header("Content-Type", "application/json")
                    .header(SIGNATURE_HEADER, signature)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                delivery.recordAttempt(true, null);
            } else {
                delivery.recordAttempt(false, "HTTP " + response.statusCode());
            }
        } catch (Exception e) {
            delivery.recordAttempt(false, e.getMessage());
        }
    }
}
