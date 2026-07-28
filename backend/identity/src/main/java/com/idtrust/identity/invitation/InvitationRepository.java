package com.idtrust.identity.invitation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvitationRepository extends JpaRepository<Invitation, UUID> {

    Optional<Invitation> findByToken(String token);

    List<Invitation> findByTenantId(UUID tenantId);

    Optional<Invitation> findByTenantIdAndId(UUID tenantId, UUID id);
}
