package com.idtrust.notification;

import com.idtrust.common.tenant.CurrentUserContext;
import com.idtrust.common.tenant.TenantContext;
import com.idtrust.common.web.ApiErrorCode;
import com.idtrust.common.web.ApiException;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * FR-NOT-002 (In-App Notification) - every endpoint here is implicitly scoped to the
 * authenticated caller's own notifications ({@link CurrentUserContext#getUserId()}); unlike
 * {@code AuditController}/{@code ApiRequestLogController}, no admin role is required, since a
 * user's own notification feed is exactly the kind of self-service data every non-admin role
 * needs.
 */
@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationRepository notificationRepository;

    public NotificationController(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @GetMapping
    public List<NotificationDtos.NotificationResponse> list(@RequestParam(defaultValue = "0") int page,
                                                              @RequestParam(defaultValue = "50") int size) {
        return notificationRepository.findByTenantIdAndUserIdOrderByCreatedAtDesc(
                        TenantContext.requireTenantId(), requireUserId(), PageRequest.of(page, Math.min(size, 200)))
                .map(NotificationDtos.NotificationResponse::from)
                .toList();
    }

    @GetMapping("/unread-count")
    public NotificationDtos.UnreadCountResponse unreadCount() {
        long count = notificationRepository.countByTenantIdAndUserIdAndReadFalse(
                TenantContext.requireTenantId(), requireUserId());
        return new NotificationDtos.UnreadCountResponse(count);
    }

    @PostMapping("/{id}/read")
    public NotificationDtos.NotificationResponse markRead(@PathVariable UUID id) {
        Notification notification = notificationRepository
                .findByTenantIdAndIdAndUserId(TenantContext.requireTenantId(), id, requireUserId())
                .orElseThrow(() -> new ApiException(ApiErrorCode.RESOURCE_NOT_FOUND, "Notification not found: " + id));
        notification.markRead();
        return NotificationDtos.NotificationResponse.from(notificationRepository.save(notification));
    }

    private UUID requireUserId() {
        UUID userId = CurrentUserContext.getUserId();
        if (userId == null) {
            throw new ApiException(ApiErrorCode.UNAUTHORIZED, "No authenticated user in context");
        }
        return userId;
    }
}
