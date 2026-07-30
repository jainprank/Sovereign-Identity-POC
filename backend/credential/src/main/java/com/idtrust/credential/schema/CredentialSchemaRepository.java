package com.idtrust.credential.schema;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CredentialSchemaRepository extends JpaRepository<CredentialSchema, UUID> {

    Optional<CredentialSchema> findByTenantIdAndId(UUID tenantId, UUID id);

    Optional<CredentialSchema> findByTenantIdAndNameAndVersion(UUID tenantId, String name, String version);

    // CAST(:name AS string) below works around a Postgres/pgjdbc parameter-type-inference failure
    // ("function lower(bytea) does not exist") that occurs when an optional String filter is bound
    // only through an "IS NULL OR ..." check with no other type context in the same prepared
    // statement - see AuditRecordRepository's analogous CAST fix for the same root cause.
    @Query("SELECT s FROM CredentialSchema s WHERE s.tenantId = :tenantId "
            + "AND (CAST(:name AS string) IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', CAST(:name AS string), '%'))) "
            + "AND (:status IS NULL OR s.status = :status)")
    List<CredentialSchema> search(@Param("tenantId") UUID tenantId, @Param("name") String name,
                                   @Param("status") SchemaStatus status);

    @Query(value = "SELECT s FROM CredentialSchema s WHERE s.tenantId = :tenantId "
            + "AND (CAST(:name AS string) IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', CAST(:name AS string), '%'))) "
            + "AND (:status IS NULL OR s.status = :status)",
            countQuery = "SELECT COUNT(s) FROM CredentialSchema s WHERE s.tenantId = :tenantId "
            + "AND (CAST(:name AS string) IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', CAST(:name AS string), '%'))) "
            + "AND (:status IS NULL OR s.status = :status)")
    Page<CredentialSchema> search(@Param("tenantId") UUID tenantId, @Param("name") String name,
                                   @Param("status") SchemaStatus status, Pageable pageable);
}
