package com.idtrust.did;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DidRepository extends JpaRepository<Did, UUID> {

    List<Did> findByTenantId(UUID tenantId);

    Page<Did> findByTenantId(UUID tenantId, Pageable pageable);

    Optional<Did> findByTenantIdAndId(UUID tenantId, UUID id);

    Optional<Did> findByDid(String did);

    Optional<Did> findByTenantIdAndDid(UUID tenantId, String did);
}
