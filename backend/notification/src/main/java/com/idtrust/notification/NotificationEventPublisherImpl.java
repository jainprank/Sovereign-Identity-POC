package com.idtrust.notification;

import com.idtrust.common.notification.NotificationEvent;
import com.idtrust.common.notification.NotificationEventPublisher;
import com.idtrust.identity.user.AppUser;
import com.idtrust.identity.user.UserRepository;
import com.idtrust.identity.user.UserStatus;
import com.idtrust.notification.email.EmailNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * The single real {@link NotificationEventPublisher} implementation (EPIC 17, FR-NOT-001/002/
 * 003/004), mirroring {@code apiplatform.webhook.WebhookEventPublisherImpl}'s "interface in
 * {@code common}, one owning-module impl" pattern.
 *
 * <p>Recipients are every {@link UserStatus#ACTIVE} user of {@code event.tenantId()} - not
 * filtered by role. Filtering to, say, only {@code ISSUER}/{@code VERIFIER} recipients would
 * require joining against the role-assignment tables {@code identity.rbac} owns, which is exactly
 * the kind of per-recipient preference FR-NOT-006 (Notification Preferences, V1-priority) is
 * meant to cover - a documented simplification for this pass, not an oversight.
 */
@Component
public class NotificationEventPublisherImpl implements NotificationEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventPublisherImpl.class);

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final EmailNotificationService emailNotificationService;

    public NotificationEventPublisherImpl(NotificationRepository notificationRepository, UserRepository userRepository,
                                           EmailNotificationService emailNotificationService) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.emailNotificationService = emailNotificationService;
    }

    @Override
    public void publish(NotificationEvent event) {
        if (event.tenantId() == null) {
            // Platform-level event with no tenant to resolve recipients against - nothing to notify.
            return;
        }
        List<AppUser> recipients = userRepository.findByTenantId(event.tenantId()).stream()
                .filter(user -> user.getStatus() == UserStatus.ACTIVE)
                .toList();

        for (AppUser recipient : recipients) {
            try {
                notificationRepository.save(new Notification(event.tenantId(), recipient.getId(), event.eventType(),
                        event.resourceType(), event.resourceId(), event.message()));
            } catch (Exception e) {
                log.warn("Failed to persist in-app notification for user={} eventType={}", recipient.getId(),
                        event.eventType(), e);
            }
            emailNotificationService.send(recipient.getEmail(), event.eventType(), event.message());
        }
    }
}
