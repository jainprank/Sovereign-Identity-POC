package com.idtrust.apiplatform.client;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApiClientRepository extends JpaRepository<ApiClient, UUID> {

    Optional<ApiClient> findByTenantIdAndId(UUID tenantId, UUID id);

    List<ApiClient> findByTenantId(UUID tenantId);

    Optional<ApiClient> findByClientIdAndSecretHash(String clientId, String secretHash);

    Optional<ApiClient> findBySecretHash(String secretHash);
}
