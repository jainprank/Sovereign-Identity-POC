package com.idtrust.common.audit;

/**
 * Publishes {@link AuditEvent}s. The default implementation ({@link LoggingAuditEventPublisher})
 * only logs; a persistent, queryable audit trail (FR-AUD-005/006) is expected to replace it once
 * the audit store is implemented, without any calling module needing to change.
 */
public interface AuditEventPublisher {

    void publish(AuditEvent event);
}
