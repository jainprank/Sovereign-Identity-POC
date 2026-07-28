package com.idtrust.credential.schema;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CredentialSchemaRepository extends JpaRepository<CredentialSchema, UUID> {

    Optional<CredentialSchema> findByTenantIdAndId(UUID tenantId, UUID id);

    Optional<CredentialSchema> findByTenantIdAndNameAndVersion(UUID tenantId, String name, String version);

    @Query("SELECT s FROM CredentialSchema s WHERE s.tenantId = :tenantId "
            + "AND (:name IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :name, '%'))) "
            + "AND (:status IS NULL OR s.status = :status)")
    List<CredentialSchema> search(@Param("tenantId") UUID tenantId, @Param("name") String name,
                                   @Param("status") SchemaStatus status);
}
