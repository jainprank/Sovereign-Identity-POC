package com.idtrust.notification;

import java.time.Instant;
import java.util.UUID;

public final class NotificationDtos {

    private NotificationDtos() {
    }

    public record NotificationResponse(UUID id, String eventType, String resourceType, UUID resourceId,
                                        String message, boolean read, Instant createdAt) {

        public static NotificationResponse from(Notification notification) {
            return new NotificationResponse(notification.getId(), notification.getEventType(),
                    notification.getResourceType(), notification.getResourceId(), notification.getMessage(),
                    notification.isRead(), notification.getCreatedAt());
        }
    }

    public record UnreadCountResponse(long unread) {
    }
}
