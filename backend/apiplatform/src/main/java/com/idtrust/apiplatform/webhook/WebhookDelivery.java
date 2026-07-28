package com.idtrust.apiplatform.webhook;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * One delivery attempt of a {@link com.idtrust.common.webhook.WebhookEvent} to a single
 * {@link WebhookEndpoint} (FR-API-008). Re-attempted in place (not a new row) by
 * {@code WebhookDeliveryService#retry} (FR-API-009), mirroring
 * {@code hedera.HederaAnchor#markConfirmed}'s "same row transitions FAILED -&gt; terminal" precedent.
 */
@Entity
@Table(name = "webhook_deliveries")
public class WebhookDelivery {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "webhook_endpoint_id", nullable = false)
    private UUID webhookEndpointId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "resource_type", nullable = false)
    private String resourceType;

    @Column(name = "resource_id", nullable = false)
    private UUID resourceId;

    @Column(nullable = false)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WebhookDeliveryStatus status;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "last_error")
    private String lastError;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected WebhookDelivery() {
        // JPA
    }

    public WebhookDelivery(UUID tenantId, UUID webhookEndpointId, String eventType, String resourceType,
                            UUID resourceId, String payload) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.webhookEndpointId = webhookEndpointId;
        this.eventType = eventType;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.payload = payload;
        this.status = WebhookDeliveryStatus.FAILED;
        this.attempts = 0;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void recordAttempt(boolean succeeded, String error) {
        this.attempts += 1;
        this.status = succeeded ? WebhookDeliveryStatus.DELIVERED : WebhookDeliveryStatus.FAILED;
        this.lastError = succeeded ? null : error;
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getWebhookEndpointId() {
        return webhookEndpointId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getResourceType() {
        return resourceType;
    }

    public UUID getResourceId() {
        return resourceId;
    }

    public String getPayload() {
        return payload;
    }

    public WebhookDeliveryStatus getStatus() {
        return status;
    }

    public int getAttempts() {
        return attempts;
    }

    public String getLastError() {
        return lastError;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
