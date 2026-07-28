package com.idtrust.identity.rbac;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Read-only access to the global permission catalogue, used to build custom roles (FR-ORG-013). */
@RestController
@RequestMapping("/api/v1/permissions")
public class PermissionController {

    private final PermissionService permissionService;

    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN')")
    public List<RbacDtos.PermissionResponse> list() {
        return permissionService.listAll().stream().map(RbacDtos.PermissionResponse::from).toList();
    }
}
