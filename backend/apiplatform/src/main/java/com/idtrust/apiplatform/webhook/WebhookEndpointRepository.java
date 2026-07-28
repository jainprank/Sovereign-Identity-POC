package com.idtrust.apiplatform.webhook;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WebhookEndpointRepository extends JpaRepository<WebhookEndpoint, UUID> {

    Optional<WebhookEndpoint> findByTenantIdAndId(UUID tenantId, UUID id);

    List<WebhookEndpoint> findByTenantId(UUID tenantId);

    List<WebhookEndpoint> findByTenantIdAndActiveTrue(UUID tenantId);
}
