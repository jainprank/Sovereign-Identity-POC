package com.idtrust.common.webhook;

/**
 * Publishes {@link WebhookEvent}s to any tenant webhook endpoint subscribed to that event type
 * (EPIC 14, FR-API-008 Webhooks). Mirrors {@code AuditEventPublisher}/{@code HederaAnchorPublisher}'s
 * decoupling exactly: every domain module calls this synchronously and unconditionally after a
 * state-changing operation, but implementations must never throw and must never block or fail the
 * calling operation if a subscriber's endpoint is unreachable - delivery failures are recorded
 * (FR-API-009 Webhook Retry) for later manual retry, never surfaced to the caller.
 *
 * <p>The single real implementation ({@code WebhookEventPublisherImpl} in the {@code apiplatform}
 * module) performs genuine HMAC-signed HTTP delivery to each matching, active
 * {@code WebhookEndpoint}.
 */
public interface WebhookEventPublisher {

    void publish(WebhookEvent event);
}
