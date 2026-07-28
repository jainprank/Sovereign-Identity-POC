package com.idtrust.identity.membership;

import com.idtrust.common.tenant.TenantContext;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Tenant-scoped API for cross-tenant memberships (a user granted access to a tenant beyond their
 * home tenant). Mutations restricted to PLATFORM_ADMIN/ORG_ADMIN, mirroring who owns role
 * assignment ({@code rbac.RoleController}) elsewhere in this module.
 */
@RestController
@RequestMapping("/api/v1/tenant-memberships")
public class TenantMembershipController {

    private final TenantMembershipService membershipService;

    public TenantMembershipController(TenantMembershipService membershipService) {
        this.membershipService = membershipService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN')")
    public ResponseEntity<MembershipDtos.Response> create(@Valid @RequestBody MembershipDtos.CreateRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        TenantMembership membership = membershipService.create(tenantId, request.userId(), request.role());
        return ResponseEntity.status(HttpStatus.CREATED).body(MembershipDtos.Response.from(membership));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN')")
    public List<MembershipDtos.Response> list() {
        UUID tenantId = TenantContext.requireTenantId();
        return membershipService.list(tenantId).stream().map(MembershipDtos.Response::from).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN')")
    public MembershipDtos.Response get(@PathVariable UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        return MembershipDtos.Response.from(membershipService.get(tenantId, id));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN')")
    public MembershipDtos.Response changeRole(@PathVariable UUID id, @Valid @RequestBody MembershipDtos.RoleChangeRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        return MembershipDtos.Response.from(membershipService.changeRole(tenantId, id, request.role()));
    }

    @PostMapping("/{id}/revoke")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN')")
    public MembershipDtos.Response revoke(@PathVariable UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        return MembershipDtos.Response.from(membershipService.revoke(tenantId, id));
    }
}
