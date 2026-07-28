package com.idtrust.did;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DidRepository extends JpaRepository<Did, UUID> {

    List<Did> findByTenantId(UUID tenantId);

    Optional<Did> findByTenantIdAndId(UUID tenantId, UUID id);

    Optional<Did> findByDid(String did);
}
