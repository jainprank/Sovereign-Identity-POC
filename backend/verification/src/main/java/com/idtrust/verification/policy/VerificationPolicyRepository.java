package com.idtrust.verification.policy;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VerificationPolicyRepository extends JpaRepository<VerificationPolicy, UUID> {

    Optional<VerificationPolicy> findByTenantIdAndId(UUID tenantId, UUID id);

    Optional<VerificationPolicy> findByTenantIdAndNameAndVersion(UUID tenantId, String name, int version);

    // CAST(:name AS string) works around a Postgres/pgjdbc parameter-type-inference failure
    // ("function lower(bytea) does not exist") - see CredentialSchemaRepository's identical fix.
    @Query("SELECT p FROM VerificationPolicy p WHERE p.tenantId = :tenantId "
            + "AND (CAST(:name AS string) IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', CAST(:name AS string), '%'))) "
            + "AND (:status IS NULL OR p.status = :status)")
    List<VerificationPolicy> search(@Param("tenantId") UUID tenantId, @Param("name") String name,
                                     @Param("status") PolicyStatus status);
}
