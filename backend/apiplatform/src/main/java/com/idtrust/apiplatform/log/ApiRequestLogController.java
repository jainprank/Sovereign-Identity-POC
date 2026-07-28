package com.idtrust.apiplatform.log;

import com.idtrust.common.tenant.TenantContext;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * FR-API-007 (API Logs) - a read-only query endpoint over the tenant's own request history.
 * Restricted to PLATFORM_ADMIN/ORG_ADMIN, mirroring {@code hedera.HederaAnchorController}'s
 * remediation/observability-endpoint precedent - request logs are operational evidence, not data
 * any non-admin role needs.
 */
@RestController
@RequestMapping("/api/v1/api-logs")
@PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN')")
public class ApiRequestLogController {

    private final ApiRequestLogRepository apiRequestLogRepository;

    public ApiRequestLogController(ApiRequestLogRepository apiRequestLogRepository) {
        this.apiRequestLogRepository = apiRequestLogRepository;
    }

    @GetMapping
    public List<ApiRequestLogDtos.ApiRequestLogResponse> list(@RequestParam(defaultValue = "0") int page,
                                                                @RequestParam(defaultValue = "50") int size) {
        PageRequest pageRequest = PageRequest.of(page, Math.min(size, 200), Sort.by("createdAt").descending());
        return apiRequestLogRepository.findByTenantIdOrderByCreatedAtDesc(TenantContext.requireTenantId(), pageRequest)
                .map(ApiRequestLogDtos.ApiRequestLogResponse::from)
                .toList();
    }
}
