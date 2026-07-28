package com.idtrust.identity.membership;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TenantMembershipRepository extends JpaRepository<TenantMembership, UUID> {

    List<TenantMembership> findByTenantId(UUID tenantId);

    List<TenantMembership> findByUserId(UUID userId);

    Optional<TenantMembership> findByTenantIdAndId(UUID tenantId, UUID id);

    Optional<TenantMembership> findByTenantIdAndUserId(UUID tenantId, UUID userId);
}
