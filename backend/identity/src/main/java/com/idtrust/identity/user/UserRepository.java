package com.idtrust.identity.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<AppUser, UUID> {

    List<AppUser> findByTenantId(UUID tenantId);

    Page<AppUser> findByTenantId(UUID tenantId, Pageable pageable);

    Optional<AppUser> findByTenantIdAndId(UUID tenantId, UUID id);

    Optional<AppUser> findByTenantIdAndKeycloakSubjectId(UUID tenantId, String keycloakSubjectId);

    Optional<AppUser> findByTenantIdAndEmail(UUID tenantId, String email);

    boolean existsByTenantIdAndEmail(UUID tenantId, String email);
}
