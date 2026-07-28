package com.idtrust.audit;

import com.idtrust.common.audit.AuditEvent;
import com.idtrust.common.audit.AuditEventPublisher;
import com.idtrust.common.webhook.WebhookEvent;
import com.idtrust.common.webhook.WebhookEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * The durable {@link AuditEventPublisher} implementation (FR-AUD-009/010) that
 * {@code common.audit.LoggingAuditEventPublisher}'s own Javadoc calls for: "replace this bean
 * with a persistent implementation when the audit store lands, without any calling module
 * needing to change". Marked {@link Primary} so this bean wins over
 * {@code LoggingAuditEventPublisher} wherever both are on the classpath (always true for the
 * {@code app} module, which now depends on {@code audit}); {@code LoggingAuditEventPublisher}
 * itself is additionally guarded with {@code @ConditionalOnMissingBean} so exactly one bean of
 * this type is ever created.
 *
 * <p>Preserves every behavior the log-only publisher already had (structured log line, webhook
 * relay of {@code AuditEventCreated}) and adds the one genuinely new thing: a row in
 * {@code audit_records}, queryable via {@link AuditController}.
 */
@Component
@Primary
public class PersistingAuditEventPublisher implements AuditEventPublisher {

    private static final Logger log = LoggerFactory.getLogger("AUDIT");

    private final AuditRecordRepository auditRecordRepository;
    private final WebhookEventPublisher webhookEventPublisher;

    public PersistingAuditEventPublisher(AuditRecordRepository auditRecordRepository,
                                          WebhookEventPublisher webhookEventPublisher) {
        this.auditRecordRepository = auditRecordRepository;
        this.webhookEventPublisher = webhookEventPublisher;
    }

    @Override
    public void publish(AuditEvent event) {
        log.info("tenant={} actor={} action={} resourceType={} resourceId={} result={} metadata={} occurredAt={}",
                event.tenantId(), event.actorUserId(), event.action(), event.resourceType(),
                event.resourceId(), event.result(), event.metadata(), event.occurredAt());
        persist(event);
        relayAsWebhookEvent(event);
    }

    private void persist(AuditEvent event) {
        try {
            auditRecordRepository.save(new AuditRecord(event.tenantId(), event.actorUserId(), event.action(),
                    event.resourceType(), event.resourceId(), event.result(), event.metadata(), event.occurredAt()));
        } catch (Exception e) {
            // Never let an audit-persistence failure affect the operation that triggered it - the
            // structured log line above is still emitted regardless, mirroring
            // WebhookEventPublisherImpl's "never throw, never block the caller" contract.
            log.error("Failed to persist audit record for action={} resourceType={} resourceId={}",
                    event.action(), event.resourceType(), event.resourceId(), e);
        }
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
