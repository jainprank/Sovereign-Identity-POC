package com.idtrust.hedera;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HederaAnchorRepository extends JpaRepository<HederaAnchor, UUID> {

    Optional<HederaAnchor> findByTenantIdAndId(UUID tenantId, UUID id);

    List<HederaAnchor> findByTenantIdAndResourceTypeAndResourceId(UUID tenantId, String resourceType, UUID resourceId);

    List<HederaAnchor> findByTenantIdAndStatus(UUID tenantId, AnchorStatus status);
}
