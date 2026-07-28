package com.idtrust.identity.tenant;

import com.idtrust.common.web.ETagSupport;
import com.idtrust.common.web.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Platform-administration API for tenants (creation happens under an organization; day-to-day
 * tenant operations are cross-tenant/platform-scoped, so no {@code X-Tenant-ID} header is
 * required here - see the API contract standard, Architecture & Implementation Baseline section
 * 9).
 */
@RestController
public class TenantController {

    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @PostMapping("/api/v1/organizations/{organizationId}/tenants")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ResponseEntity<TenantDtos.Response> create(@PathVariable UUID organizationId,
                                                        @Valid @RequestBody TenantDtos.CreateRequest request) {
        Tenant tenant = tenantService.create(organizationId, request.name());
        return ResponseEntity.status(HttpStatus.CREATED).body(TenantDtos.Response.from(tenant));
    }

    @GetMapping("/api/v1/organizations/{organizationId}/tenants")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN')")
    public PageResponse<TenantDtos.Response> listByOrganization(@PathVariable UUID organizationId,
                                                                   @RequestParam(defaultValue = "0") int page,
                                                                   @RequestParam(defaultValue = "50") int size) {
        return PageResponse.of(tenantService.listByOrganization(organizationId, PageRequest.of(page, Math.min(size, 200))),
                TenantDtos.Response::from);
    }

    // Spec's flat `GET /api/v1/tenants` (list-all-tenants-across-organizations), alongside the
    // org-nested listing above which remains the primary creation/listing path for a specific
    // organization's own tenants.
    @GetMapping("/api/v1/tenants")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public PageResponse<TenantDtos.Response> listAll(@RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "50") int size) {
        return PageResponse.of(tenantService.listAll(PageRequest.of(page, Math.min(size, 200))), TenantDtos.Response::from);
    }

    @GetMapping("/api/v1/tenants/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN')")
    public ResponseEntity<TenantDtos.Response> get(@PathVariable UUID id) {
        Tenant tenant = tenantService.get(id);
        return ResponseEntity.ok()
                .eTag(ETagSupport.toHeaderValue(tenant.getLockVersion()))
                .body(TenantDtos.Response.from(tenant));
    }

    @PatchMapping("/api/v1/tenants/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN')")
    public ResponseEntity<TenantDtos.Response> update(@PathVariable UUID id,
                                                        @RequestHeader(value = "If-Match", required = false) String ifMatch,
                                                        @RequestBody TenantDtos.UpdateRequest request) {
        ETagSupport.requireMatch(ifMatch, tenantService.get(id).getLockVersion());
        Tenant tenant = tenantService.update(id, request.name());
        return ResponseEntity.ok()
                .eTag(ETagSupport.toHeaderValue(tenant.getLockVersion()))
                .body(TenantDtos.Response.from(tenant));
    }

    @PutMapping("/api/v1/tenants/{id}/settings")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN')")
    public TenantDtos.Response updateSettings(@PathVariable UUID id, @RequestBody TenantDtos.ConfigurationRequest request) {
        return TenantDtos.Response.from(tenantService.updateConfiguration(id, request.configuration()));
    }

    @PostMapping("/api/v1/tenants/{id}/suspend")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN')")
    public TenantDtos.Response suspend(@PathVariable UUID id) {
        return TenantDtos.Response.from(tenantService.suspend(id));
    }

    @PostMapping("/api/v1/tenants/{id}/reactivate")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN')")
    public TenantDtos.Response reactivate(@PathVariable UUID id) {
        return TenantDtos.Response.from(tenantService.reactivate(id));
    }
}
