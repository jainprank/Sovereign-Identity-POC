package com.idtrust.wallet.oid4vci;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface Oid4vciSessionRepository extends JpaRepository<Oid4vciSession, UUID> {

    Optional<Oid4vciSession> findByTenantIdAndPreAuthorizedCode(UUID tenantId, String preAuthorizedCode);

    Optional<Oid4vciSession> findByTenantIdAndAccessToken(UUID tenantId, String accessToken);
}
