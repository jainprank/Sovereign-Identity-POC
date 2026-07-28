package com.idtrust.common.audit;

import com.idtrust.common.webhook.WebhookEvent;
import com.idtrust.common.webhook.WebhookEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Default {@link AuditEventPublisher} used until a durable audit store is wired up. Every
 * event is still emitted as a structured log line so nothing is silently dropped during early
 * epics; replace this bean with a persistent implementation when the audit store lands.
 *
 * <p>Also relays every audit event as an {@code AuditEventCreated} webhook (EPIC 14, FR-API-008)
 * via {@link WebhookEventPublisher} - this single choke point, rather than threading webhook
 * calls through every module that publishes an {@link AuditEvent}, is deliberate: it guarantees
 * every audited mutation across the whole codebase is relayed with zero per-module wiring.
 * Skipped for platform-level events with no {@code tenantId} (nothing to look up subscribers
 * against) or a non-UUID {@code resourceId} (e.g. an email address), mirroring
 * {@code webhook.WebhookEventPublisherImpl}'s own "never throw, never block the caller" contract.
 *
 * <p>As of E15, {@code audit.PersistingAuditEventPublisher} is exactly the persistent
 * implementation this Javadoc called for and is always present once the {@code audit} module is
 * on the classpath (every real deployment, since {@code app} depends on it) - this class now only
 * activates via {@link ConditionalOnMissingBean} as a fallback for contexts that don't load the
 * {@code audit} module (e.g. a narrowly-sliced {@code @SpringBootTest}).
 */
@Component
@ConditionalOnMissingBean(AuditEventPublisher.class)
public class LoggingAuditEventPublisher implements AuditEventPublisher {

    private static final Logger log = LoggerFactory.getLogger("AUDIT");

    private final WebhookEventPublisher webhookEventPublisher;

    public LoggingAuditEventPublisher(WebhookEventPublisher webhookEventPublisher) {
        this.webhookEventPublisher = webhookEventPublisher;
    }

    @Override
    public void publish(AuditEvent event) {
        log.info("tenant={} actor={} action={} resourceType={} resourceId={} result={} metadata={} occurredAt={}",
                event.tenantId(), event.actorUserId(), event.action(), event.resourceType(),
                event.resourceId(), event.result(), event.metadata(), event.occurredAt());
        relayAsWebhookEvent(event);
    }

    private void relayAsWebhookEvent(AuditEvent event) {
        try {
            if (event.tenantId() == null || event.resourceId() == null) {
                return;
            }
            UUID resourceId = UUID.fromString(event.resourceId());
            webhookEventPublisher.publish(WebhookEvent.of(event.tenantId(), "AuditEventCreated", event.resourceType(),
                    resourceId, Map.of("action", event.action(), "result", event.result())));
        } catch (IllegalArgumentException e) {
            // resourceId wasn't a UUID (e.g. a platform-level action keyed by email) - nothing to relay.
        }
    }
}
