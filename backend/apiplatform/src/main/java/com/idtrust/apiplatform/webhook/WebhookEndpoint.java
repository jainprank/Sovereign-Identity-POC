package com.idtrust.apiplatform.webhook;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * A tenant-owned outbound webhook subscription (FR-API-008). {@code encryptedSecret} is the HMAC
 * signing secret, envelope-encrypted at rest via {@code kms.crypto.KeyEncryptionService} - the
 * same AES-256 KEK scheme already used for private key material - because, unlike
 * {@code client.ApiClient.secretHash}, this secret must be recoverable in plaintext to compute the
 * outbound {@code X-IDTrust-Signature} HMAC on every delivery, so it cannot merely be hashed.
 * {@code eventTypes} is a comma-separated subset of the Kafka event-type names already catalogued
 * in the Baseline's section 6 (e.g. {@code CredentialIssued}, {@code DIDCreated}), reused here as
 * the webhook subscription vocabulary.
 */
@Entity
@Table(name = "webhook_endpoints")
public class WebhookEndpoint {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private String url;

    @Column(name = "event_types", nullable = false)
    private String eventTypes;

    @Column(name = "encrypted_secret", nullable = false)
    private String encryptedSecret;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected WebhookEndpoint() {
        // JPA
    }

    public WebhookEndpoint(UUID tenantId, String url, List<String> eventTypes, String encryptedSecret) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.url = url;
        this.eventTypes = String.join(",", eventTypes);
        this.encryptedSecret = encryptedSecret;
        this.active = true;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public boolean subscribesTo(String eventType) {
        return active && List.of(eventTypes.split(",")).contains(eventType);
    }

    public void deactivate() {
        this.active = false;
        this.updatedAt = Instant.now();
    }

    public void activate() {
        this.active = true;
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getUrl() {
        return url;
    }

    public List<String> getEventTypes() {
        return List.of(eventTypes.split(","));
    }

    public String getEncryptedSecret() {
        return encryptedSecret;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
