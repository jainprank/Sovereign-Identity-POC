package com.idtrust.apiplatform.log;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ApiRequestLogRepository extends JpaRepository<ApiRequestLog, UUID> {

    Page<ApiRequestLog> findByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);
}
