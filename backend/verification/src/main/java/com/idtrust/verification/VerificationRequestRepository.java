package com.idtrust.verification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VerificationRequestRepository extends JpaRepository<VerificationRequest, UUID> {

    Optional<VerificationRequest> findByTenantIdAndId(UUID tenantId, UUID id);

    @Query("SELECT r FROM VerificationRequest r WHERE r.tenantId = :tenantId "
            + "AND (:status IS NULL OR r.status = :status) ORDER BY r.createdAt DESC")
    List<VerificationRequest> search(@Param("tenantId") UUID tenantId, @Param("status") VerificationStatus status);
}
