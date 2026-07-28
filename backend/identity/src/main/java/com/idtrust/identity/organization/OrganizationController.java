package com.idtrust.identity.organization;

import com.idtrust.common.web.ETagSupport;
import com.idtrust.common.web.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Platform-administration API for organizations. Not tenant-scoped: callers act across the whole
 * platform, so no {@code X-Tenant-ID} header is required (Master Specification, section 24 "API
 * Specification", "Identity").
 */
@RestController
@RequestMapping("/api/v1/organizations")
public class OrganizationController {

    private final OrganizationService organizationService;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @PostMapping
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ResponseEntity<OrganizationDtos.Response> create(@Valid @RequestBody OrganizationDtos.CreateRequest request) {
        Organization organization = organizationService.create(request.name(), request.description(), request.contactEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(OrganizationDtos.Response.from(organization));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN')")
    public PageResponse<OrganizationDtos.Response> list(@RequestParam(defaultValue = "0") int page,
                                                          @RequestParam(defaultValue = "50") int size) {
        return PageResponse.of(organizationService.listAll(PageRequest.of(page, Math.min(size, 200))),
                OrganizationDtos.Response::from);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN')")
    public ResponseEntity<OrganizationDtos.Response> get(@PathVariable UUID id) {
        Organization organization = organizationService.get(id);
        return ResponseEntity.ok()
                .eTag(ETagSupport.toHeaderValue(organization.getLockVersion()))
                .body(OrganizationDtos.Response.from(organization));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN')")
    public ResponseEntity<OrganizationDtos.Response> update(@PathVariable UUID id,
                                                              @RequestHeader(value = "If-Match", required = false) String ifMatch,
                                                              @RequestBody OrganizationDtos.UpdateRequest request) {
        ETagSupport.requireMatch(ifMatch, organizationService.get(id).getLockVersion());
        Organization organization = organizationService.update(
                id, request.name(), request.description(), request.logoUrl(), request.website(), request.contactEmail());
        return ResponseEntity.ok()
                .eTag(ETagSupport.toHeaderValue(organization.getLockVersion()))
                .body(OrganizationDtos.Response.from(organization));
    }

    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public OrganizationDtos.Response deactivate(@PathVariable UUID id) {
        return OrganizationDtos.Response.from(organizationService.deactivate(id));
    }
}
