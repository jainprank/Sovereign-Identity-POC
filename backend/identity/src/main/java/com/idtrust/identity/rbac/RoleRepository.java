package com.idtrust.identity.rbac;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<Role, UUID> {

    /** System (global) roles plus the tenant's own custom roles - the full set visible to a tenant. */
    List<Role> findByTenantIdIsNullOrTenantId(UUID tenantId);

    Optional<Role> findByTenantIdAndCode(UUID tenantId, String code);

    Optional<Role> findByTenantIdIsNullAndCode(String code);

    boolean existsByTenantIdAndCode(UUID tenantId, String code);
}
