package com.idtrust.trust;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TrustedIssuerRepository extends JpaRepository<TrustedIssuer, UUID> {

    Optional<TrustedIssuer> findByTenantIdAndId(UUID tenantId, UUID id);

    List<TrustedIssuer> findByTenantIdAndTrustRegistryId(UUID tenantId, UUID trustRegistryId);

    List<TrustedIssuer> findByTenantIdAndIssuerDid(UUID tenantId, String issuerDid);

    boolean existsByTenantId(UUID tenantId);
}
