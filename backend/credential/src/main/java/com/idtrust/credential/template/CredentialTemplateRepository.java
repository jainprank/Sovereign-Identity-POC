package com.idtrust.credential.template;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CredentialTemplateRepository extends JpaRepository<CredentialTemplate, UUID> {

    Optional<CredentialTemplate> findByTenantIdAndId(UUID tenantId, UUID id);

    List<CredentialTemplate> findByTenantId(UUID tenantId);

    Page<CredentialTemplate> findByTenantId(UUID tenantId, Pageable pageable);

    Optional<CredentialTemplate> findByTenantIdAndName(UUID tenantId, String name);
}
