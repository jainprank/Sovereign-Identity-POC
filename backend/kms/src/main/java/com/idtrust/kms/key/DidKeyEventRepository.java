package com.idtrust.kms.key;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DidKeyEventRepository extends JpaRepository<DidKeyEvent, UUID> {

    List<DidKeyEvent> findByTenantIdAndDidIdOrderByOccurredAtDesc(UUID tenantId, UUID didId);

    List<DidKeyEvent> findByTenantIdAndKeyIdOrderByOccurredAtDesc(UUID tenantId, UUID keyId);
}
