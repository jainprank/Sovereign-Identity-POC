package com.idtrust.identity.idempotency;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, UUID> {

    Optional<IdempotencyKey> findByTenantIdAndIdempotencyKeyAndEndpoint(UUID tenantId, String idempotencyKey, String endpoint);

    void deleteByExpiresAtBefore(Instant cutoff);
}
