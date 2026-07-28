package com.idtrust.notification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Page<Notification> findByTenantIdAndUserIdOrderByCreatedAtDesc(UUID tenantId, UUID userId, Pageable pageable);

    Optional<Notification> findByTenantIdAndIdAndUserId(UUID tenantId, UUID id, UUID userId);

    long countByTenantIdAndUserIdAndReadFalse(UUID tenantId, UUID userId);
}
