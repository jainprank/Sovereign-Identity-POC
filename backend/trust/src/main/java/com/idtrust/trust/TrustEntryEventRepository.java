package com.idtrust.trust;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TrustEntryEventRepository extends JpaRepository<TrustEntryEvent, UUID> {

    List<TrustEntryEvent> findByTenantIdAndTrustedIssuerIdOrderByOccurredAtDesc(UUID tenantId, UUID trustedIssuerId);
}
