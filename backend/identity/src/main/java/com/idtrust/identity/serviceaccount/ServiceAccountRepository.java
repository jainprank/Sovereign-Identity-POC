package com.idtrust.identity.serviceaccount;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ServiceAccountRepository extends JpaRepository<ServiceAccount, UUID> {

    Page<ServiceAccount> findByTenantId(UUID tenantId, Pageable pageable);

    Optional<ServiceAccount> findByTenantIdAndId(UUID tenantId, UUID id);
}
