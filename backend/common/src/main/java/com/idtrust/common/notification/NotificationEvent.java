package com.idtrust.common.notification;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * A tenant-wide event eligible for notification (EPIC 17, FR-NOT-003/004 - Issuance/Verification
 * Notification). Unlike {@code AuditEvent}/{@code WebhookEvent}, which target a system or an
 * external subscriber, a {@code NotificationEvent} has human recipients: the single real
 * {@link NotificationEventPublisher} implementation ({@code notification.NotificationEventPublisherImpl})
 * resolves every active user of {@code tenantId} and creates one in-app notification plus one
 * best-effort email per recipient - see its Javadoc for why recipients aren't filtered by role in
 * this pass.
 *
 * @param message human-readable summary shown in-app and in the email body
 */
public record NotificationEvent(
        UUID tenantId,
        String eventType,
        String resourceType,
        UUID resourceId,
        String message,
        Map<String, Object> metadata,
        Instant occurredAt
) {

    public static NotificationEvent of(UUID tenantId, String eventType, String resourceType, UUID resourceId,
                                        String message, Map<String, Object> metadata) {
        return new NotificationEvent(tenantId, eventType, resourceType, resourceId, message, metadata, Instant.now());
    }
}
