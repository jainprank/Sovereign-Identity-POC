package com.idtrust.identity.serviceaccount;

import com.idtrust.common.tenant.TenantContext;
import com.idtrust.common.web.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Tenant-scoped API for service accounts (non-human identities). Mutations restricted to
 * PLATFORM_ADMIN/ORG_ADMIN, mirroring who owns user/role administration elsewhere in this module.
 */
@RestController
@RequestMapping("/api/v1/service-accounts")
public class ServiceAccountController {

    private final ServiceAccountService serviceAccountService;

    public ServiceAccountController(ServiceAccountService serviceAccountService) {
        this.serviceAccountService = serviceAccountService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN')")
    public ResponseEntity<ServiceAccountDtos.Response> create(@Valid @RequestBody ServiceAccountDtos.CreateRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        ServiceAccount serviceAccount = serviceAccountService.create(tenantId, request.name(), request.description());
        return ResponseEntity.status(HttpStatus.CREATED).body(ServiceAccountDtos.Response.from(serviceAccount));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN')")
    public PageResponse<ServiceAccountDtos.Response> list(@RequestParam(defaultValue = "0") int page,
                                                             @RequestParam(defaultValue = "50") int size) {
        UUID tenantId = TenantContext.requireTenantId();
        return PageResponse.of(serviceAccountService.list(tenantId, PageRequest.of(page, Math.min(size, 200))),
                ServiceAccountDtos.Response::from);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN')")
    public ServiceAccountDtos.Response get(@PathVariable UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        return ServiceAccountDtos.Response.from(serviceAccountService.get(tenantId, id));
    }

    @PostMapping("/{id}/suspend")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN')")
    public ServiceAccountDtos.Response suspend(@PathVariable UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        return ServiceAccountDtos.Response.from(serviceAccountService.suspend(tenantId, id));
    }

    @PostMapping("/{id}/reactivate")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN')")
    public ServiceAccountDtos.Response reactivate(@PathVariable UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        return ServiceAccountDtos.Response.from(serviceAccountService.reactivate(tenantId, id));
    }

    @PostMapping("/{id}/revoke")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN')")
    public ServiceAccountDtos.Response revoke(@PathVariable UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        return ServiceAccountDtos.Response.from(serviceAccountService.revoke(tenantId, id));
    }
}
