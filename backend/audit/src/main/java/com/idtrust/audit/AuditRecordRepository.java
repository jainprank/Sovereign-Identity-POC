package com.idtrust.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface AuditRecordRepository extends JpaRepository<AuditRecord, UUID> {

    /**
     * FR-AUD-009 (Search Audit) - every filter beyond {@code tenantId} is optional. {@code tenantId}
     * itself is always supplied by the caller ({@link AuditController}, from
     * {@link com.idtrust.common.tenant.TenantContext#requireTenantId()}) in addition to being
     * enforced by row-level security ({@code V23__audit_row_level_security.sql}) - defense in
     * depth, matching every other tenant-scoped repository in this codebase.
     */
    @Query("""
            SELECT a FROM AuditRecord a
            WHERE a.tenantId = :tenantId
            AND (:action IS NULL OR a.action = :action)
            AND (:resourceType IS NULL OR a.resourceType = :resourceType)
            AND (:actorUserId IS NULL OR a.actorUserId = :actorUserId)
            AND (:from IS NULL OR a.occurredAt >= :from)
            AND (:to IS NULL OR a.occurredAt <= :to)
            ORDER BY a.occurredAt DESC
            """)
    Page<AuditRecord> search(@Param("tenantId") UUID tenantId, @Param("action") String action,
                              @Param("resourceType") String resourceType, @Param("actorUserId") UUID actorUserId,
                              @Param("from") Instant from, @Param("to") Instant to, Pageable pageable);
}
