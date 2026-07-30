package com.idtrust.identity.rbac;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<Role, UUID> {

    /**
     * System (global) roles plus the tenant's own custom roles - the full set visible to a
     * tenant. Uses a fetch join so the lazy {@code permissions} collection is populated before the
     * transaction closes, avoiding a {@code LazyInitializationException} when mapping to a DTO in
     * the controller layer.
     */
    @Query("SELECT DISTINCT r FROM Role r LEFT JOIN FETCH r.permissions WHERE r.tenantId IS NULL OR r.tenantId = :tenantId")
    List<Role> findByTenantIdIsNullOrTenantId(@Param("tenantId") UUID tenantId);

    @Query("SELECT r FROM Role r LEFT JOIN FETCH r.permissions WHERE r.id = :id")
    Optional<Role> findByIdFetchingPermissions(@Param("id") UUID id);

    Optional<Role> findByTenantIdAndCode(UUID tenantId, String code);

    Optional<Role> findByTenantIdIsNullAndCode(String code);

    boolean existsByTenantIdAndCode(UUID tenantId, String code);
}
