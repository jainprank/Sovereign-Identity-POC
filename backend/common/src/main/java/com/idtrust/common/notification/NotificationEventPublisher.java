package com.idtrust.common.notification;

/**
 * Publishes {@link NotificationEvent}s (EPIC 17, FR-NOT-003/004). Mirrors
 * {@code AuditEventPublisher}/{@code WebhookEventPublisher}/{@code HederaAnchorPublisher}'s
 * decoupling exactly: every domain module calls this synchronously and unconditionally after a
 * state-changing operation, and the single real implementation
 * ({@code notification.NotificationEventPublisherImpl} in the {@code notification} module) must
 * never throw and must never block or fail the calling operation - an undeliverable email or a
 * notification-persistence failure is logged and swallowed, never surfaced to the caller.
 */
public interface NotificationEventPublisher {

    void publish(NotificationEvent event);
}
