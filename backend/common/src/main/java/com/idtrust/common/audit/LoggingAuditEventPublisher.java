package com.idtrust.common.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Default {@link AuditEventPublisher} used until a durable audit store is wired up. Every
 * event is still emitted as a structured log line so nothing is silently dropped during early
 * epics; replace this bean with a persistent implementation when the audit store lands.
 */
@Component
public class LoggingAuditEventPublisher implements AuditEventPublisher {

    private static final Logger log = LoggerFactory.getLogger("AUDIT");

    @Override
    public void publish(AuditEvent event) {
        log.info("tenant={} actor={} action={} resourceType={} resourceId={} result={} metadata={} occurredAt={}",
                event.tenantId(), event.actorUserId(), event.action(), event.resourceType(),
                event.resourceId(), event.result(), event.metadata(), event.occurredAt());
    }
}
