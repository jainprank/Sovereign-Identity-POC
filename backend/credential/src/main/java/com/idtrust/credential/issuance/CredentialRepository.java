package com.idtrust.credential.issuance;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CredentialRepository extends JpaRepository<Credential, UUID> {

    Optional<Credential> findByTenantIdAndId(UUID tenantId, UUID id);

    List<Credential> findByTenantId(UUID tenantId);

    List<Credential> findByTenantIdAndSchemaId(UUID tenantId, UUID schemaId);

    @Query("""
            SELECT c FROM Credential c
            WHERE c.tenantId = :tenantId
            AND (:subjectDid IS NULL OR c.subjectDid = :subjectDid)
            AND (:status IS NULL OR c.status = :status)
            ORDER BY c.createdAt DESC
            """)
    List<Credential> search(@Param("tenantId") UUID tenantId, @Param("subjectDid") String subjectDid,
                             @Param("status") CredentialStatus status);

    @Query(value = """
            SELECT c FROM Credential c
            WHERE c.tenantId = :tenantId
            AND (:subjectDid IS NULL OR c.subjectDid = :subjectDid)
            AND (:status IS NULL OR c.status = :status)
            ORDER BY c.createdAt DESC
            """,
            countQuery = """
            SELECT COUNT(c) FROM Credential c
            WHERE c.tenantId = :tenantId
            AND (:subjectDid IS NULL OR c.subjectDid = :subjectDid)
            AND (:status IS NULL OR c.status = :status)
            """)
    Page<Credential> search(@Param("tenantId") UUID tenantId, @Param("subjectDid") String subjectDid,
                             @Param("status") CredentialStatus status, Pageable pageable);
}
