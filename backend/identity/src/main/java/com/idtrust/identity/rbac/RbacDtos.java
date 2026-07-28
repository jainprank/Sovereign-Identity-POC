package com.idtrust.identity.rbac;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class RbacDtos {

    private RbacDtos() {
    }

    public record CreateRoleRequest(
            @NotBlank String code,
            @NotBlank String name,
            String description,
            Set<UUID> permissionIds
    ) {
    }

    public record PermissionResponse(UUID id, String resource, String action, String description) {
        public static PermissionResponse from(Permission permission) {
            return new PermissionResponse(permission.getId(), permission.getResource(), permission.getAction(), permission.getDescription());
        }
    }

    public record RoleResponse(
            UUID id,
            UUID tenantId,
            String code,
            String name,
            String description,
            boolean system,
            Instant createdAt,
            List<PermissionResponse> permissions
    ) {
        public static RoleResponse from(Role role) {
            return new RoleResponse(
                    role.getId(),
                    role.getTenantId(),
                    role.getCode(),
                    role.getName(),
                    role.getDescription(),
                    role.isSystem(),
                    role.getCreatedAt(),
                    role.getPermissions().stream().map(PermissionResponse::from).toList()
            );
        }
    }

    public record AssignRoleRequest(@NotNull UUID userId, @NotNull UUID roleId) {
    }

    public record UserRoleResponse(UUID id, UUID userId, UUID roleId, UUID tenantId, Instant assignedAt) {
        public static UserRoleResponse from(UserRole userRole) {
            return new UserRoleResponse(
                    userRole.getId(),
                    userRole.getUserId(),
                    userRole.getRoleId(),
                    userRole.getTenantId(),
                    userRole.getAssignedAt()
            );
        }
    }
}
