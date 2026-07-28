package com.idtrust.identity.rbac;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRoleRepository extends JpaRepository<UserRole, UUID> {

    List<UserRole> findByTenantIdAndUserId(UUID tenantId, UUID userId);

    Optional<UserRole> findByTenantIdAndUserIdAndRoleId(UUID tenantId, UUID userId, UUID roleId);

    Optional<UserRole> findByTenantIdAndId(UUID tenantId, UUID id);
}
