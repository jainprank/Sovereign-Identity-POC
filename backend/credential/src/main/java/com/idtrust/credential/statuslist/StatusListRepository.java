package com.idtrust.credential.statuslist;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface StatusListRepository extends JpaRepository<StatusList, UUID> {

    Optional<StatusList> findByTenantIdAndIssuerDidAndPurpose(UUID tenantId, String issuerDid, StatusListPurpose purpose);

    Optional<StatusList> findByTenantIdAndId(UUID tenantId, UUID id);
}
