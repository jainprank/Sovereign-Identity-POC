package com.idtrust.apiplatform.webhook;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WebhookDeliveryRepository extends JpaRepository<WebhookDelivery, UUID> {

    Optional<WebhookDelivery> findByTenantIdAndId(UUID tenantId, UUID id);

    List<WebhookDelivery> findByTenantIdAndWebhookEndpointId(UUID tenantId, UUID webhookEndpointId);
}
