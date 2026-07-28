package com.idtrust.trust;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TrustRegistryRepository extends JpaRepository<TrustRegistry, UUID> {

    Optional<TrustRegistry> findByTenantIdAndId(UUID tenantId, UUID id);

    List<TrustRegistry> findByTenantId(UUID tenantId);

    @Query("SELECT COUNT(r) > 0 FROM TrustRegistry r WHERE r.tenantId = :tenantId")
    boolean existsByTenantId(@Param("tenantId") UUID tenantId);
}
