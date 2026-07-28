package com.idtrust.identity.invitation;

import com.idtrust.common.tenant.TenantContext;
import com.idtrust.identity.user.UserDtos;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/invitations")
public class InvitationController {

    private final InvitationService invitationService;

    public InvitationController(InvitationService invitationService) {
        this.invitationService = invitationService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN')")
    public ResponseEntity<InvitationDtos.Response> create(@Valid @RequestBody InvitationDtos.CreateRequest request) {
        Invitation invitation = invitationService.create(TenantContext.requireTenantId(), request.email(), request.roleId());
        return ResponseEntity.status(HttpStatus.CREATED).body(InvitationDtos.Response.from(invitation));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN')")
    public List<InvitationDtos.Response> list() {
        return invitationService.listByTenant(TenantContext.requireTenantId()).stream().map(InvitationDtos.Response::from).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN')")
    public InvitationDtos.Response get(@PathVariable UUID id) {
        return InvitationDtos.Response.from(invitationService.get(TenantContext.requireTenantId(), id));
    }

    @PostMapping("/{id}/revoke")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN')")
    public InvitationDtos.Response revoke(@PathVariable UUID id) {
        return InvitationDtos.Response.from(invitationService.revoke(TenantContext.requireTenantId(), id));
    }

    /**
     * Public endpoint (no {@code X-Tenant-ID} header, no authentication): the invited party is not
     * yet a tenant member. Permitted anonymously by {@code SecurityConfig}; the tenant is resolved
     * from the invitation token itself.
     */
    @PostMapping("/accept")
    public UserDtos.Response accept(@Valid @RequestBody InvitationDtos.AcceptRequest request) {
        return UserDtos.Response.from(invitationService.accept(request.token(), request.displayName(), request.keycloakSubjectId()));
    }
}
