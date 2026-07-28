package com.idtrust.kms.key;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ManagedKeyRepository extends JpaRepository<ManagedKey, UUID> {

    Optional<ManagedKey> findByTenantIdAndId(UUID tenantId, UUID id);

    List<ManagedKey> findByDidId(UUID didId);
}
