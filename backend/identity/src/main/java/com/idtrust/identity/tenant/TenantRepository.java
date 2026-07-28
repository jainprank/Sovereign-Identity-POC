package com.idtrust.identity.tenant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    List<Tenant> findByOrganizationId(UUID organizationId);

    Page<Tenant> findByOrganizationId(UUID organizationId, Pageable pageable);
}
