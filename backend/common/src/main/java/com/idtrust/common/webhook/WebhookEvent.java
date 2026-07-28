package com.idtrust.common.webhook;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * A domain event eligible for relay to a tenant's subscribed {@code WebhookEndpoint}s (EPIC 14,
 * FR-API-008 Webhooks). {@code eventType} is one of the Kafka event-type names already catalogued
 * in the Architecture &amp; Implementation Baseline, section 6 ("Inter-Service Communication") -
 * e.g. {@code CredentialIssued}, {@code CredentialRevoked}, {@code DIDCreated},
 * {@code TrustRegistryUpdated} - reused here as webhook subscription topics rather than inventing
 * a separate vocabulary, since no Kafka broker exists anywhere in this codebase yet.
 *
 * @param payload additional structured context relayed to the subscriber (kept small; never place
 *                private key material in here - mirrors {@code AuditEvent.metadata()}'s precedent)
 */
public record WebhookEvent(
        UUID tenantId,
        String eventType,
        String resourceType,
        UUID resourceId,
        Map<String, Object> payload,
        Instant occurredAt
) {

    public static WebhookEvent of(UUID tenantId, String eventType, String resourceType, UUID resourceId,
                                   Map<String, Object> payload) {
        return new WebhookEvent(tenantId, eventType, resourceType, resourceId, payload, Instant.now());
    }
}
