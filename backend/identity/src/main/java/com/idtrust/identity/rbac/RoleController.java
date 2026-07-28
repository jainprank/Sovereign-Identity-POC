package com.idtrust.identity.rbac;

import com.idtrust.common.tenant.TenantContext;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Tenant-scoped role and role-assignment API (requires {@code X-Tenant-ID}). Global system roles
 * (PLATFORM_ADMIN, ORG_ADMIN, ...) are always included when listing/reading, alongside the
 * tenant's own custom roles.
 */
@RestController
@RequestMapping("/api/v1/roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN')")
    public List<RbacDtos.RoleResponse> list() {
        return roleService.listAvailable(TenantContext.requireTenantId()).stream().map(RbacDtos.RoleResponse::from).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN')")
    public RbacDtos.RoleResponse get(@PathVariable UUID id) {
        return RbacDtos.RoleResponse.from(roleService.get(TenantContext.requireTenantId(), id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN')")
    public ResponseEntity<RbacDtos.RoleResponse> create(@Valid @RequestBody RbacDtos.CreateRoleRequest request) {
        Role role = roleService.create(TenantContext.requireTenantId(), request.code(), request.name(),
                request.description(), request.permissionIds());
        return ResponseEntity.status(HttpStatus.CREATED).body(RbacDtos.RoleResponse.from(role));
    }

    @PostMapping("/assignments")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN')")
    public ResponseEntity<RbacDtos.UserRoleResponse> assign(@Valid @RequestBody RbacDtos.AssignRoleRequest request) {
        UserRole userRole = roleService.assign(TenantContext.requireTenantId(), request.userId(), request.roleId());
        return ResponseEntity.status(HttpStatus.CREATED).body(RbacDtos.UserRoleResponse.from(userRole));
    }

    @DeleteMapping("/assignments/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN')")
    public ResponseEntity<Void> revoke(@PathVariable UUID id) {
        roleService.revoke(TenantContext.requireTenantId(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/assignments/users/{userId}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN')")
    public List<RbacDtos.UserRoleResponse> listUserRoles(@PathVariable UUID userId) {
        return roleService.listUserRoles(TenantContext.requireTenantId(), userId).stream()
                .map(RbacDtos.UserRoleResponse::from).toList();
    }
}
