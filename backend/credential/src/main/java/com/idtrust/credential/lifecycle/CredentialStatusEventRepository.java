package com.idtrust.credential.lifecycle;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CredentialStatusEventRepository extends JpaRepository<CredentialStatusEvent, UUID> {

    List<CredentialStatusEvent> findByTenantIdAndCredentialIdOrderByCreatedAtDesc(UUID tenantId, UUID credentialId);
}
