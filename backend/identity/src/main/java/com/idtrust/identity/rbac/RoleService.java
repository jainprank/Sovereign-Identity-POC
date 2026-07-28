package com.idtrust.identity.rbac;

import com.idtrust.common.audit.AuditEvent;
import com.idtrust.common.audit.AuditEventPublisher;
import com.idtrust.common.tenant.CurrentUserContext;
import com.idtrust.common.web.ApiErrorCode;
import com.idtrust.common.web.ApiException;
import com.idtrust.identity.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Implements FR-ORG-011 (Create Role), FR-ORG-012 (Assign Role) and FR-ORG-013 (Custom
 * Permission Sets - a tenant role is created with a hand-picked subset of the global permission
 * catalogue).
 */
@Service
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserRepository userRepository;
    private final AuditEventPublisher auditEventPublisher;

    public RoleService(RoleRepository roleRepository, PermissionRepository permissionRepository,
                        UserRoleRepository userRoleRepository, UserRepository userRepository,
                        AuditEventPublisher auditEventPublisher) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.userRoleRepository = userRoleRepository;
        this.userRepository = userRepository;
        this.auditEventPublisher = auditEventPublisher;
    }

    /** System (global) roles plus the tenant's own custom roles. */
    public List<Role> listAvailable(UUID tenantId) {
        return roleRepository.findByTenantIdIsNullOrTenantId(tenantId);
    }

    public Role get(UUID tenantId, UUID id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ApiException(ApiErrorCode.RESOURCE_NOT_FOUND, "Role not found: " + id));
        if (role.getTenantId() != null && !role.getTenantId().equals(tenantId)) {
            throw new ApiException(ApiErrorCode.RESOURCE_NOT_FOUND, "Role not found: " + id);
        }
        return role;
    }

    @Transactional
    public Role create(UUID tenantId, String code, String name, String description, Set<UUID> permissionIds) {
        if (roleRepository.existsByTenantIdAndCode(tenantId, code)) {
            throw new ApiException(ApiErrorCode.RESOURCE_CONFLICT, "A role with this code already exists in the tenant: " + code);
        }
        Role role = new Role(tenantId, code, name, description);
        if (permissionIds != null) {
            for (Permission permission : permissionRepository.findAllById(permissionIds)) {
                role.grant(permission);
            }
        }
        roleRepository.save(role);
        audit(tenantId, "ROLE_CREATED", role.getId());
        return role;
    }

    @Transactional
    public UserRole assign(UUID tenantId, UUID userId, UUID roleId) {
        userRepository.findByTenantIdAndId(tenantId, userId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.RESOURCE_NOT_FOUND, "User not found: " + userId));
        get(tenantId, roleId);
        if (userRoleRepository.findByTenantIdAndUserIdAndRoleId(tenantId, userId, roleId).isPresent()) {
            throw new ApiException(ApiErrorCode.RESOURCE_CONFLICT, "User already has this role assigned");
        }
        UserRole userRole = new UserRole(userId, roleId, tenantId);
        userRoleRepository.save(userRole);
        audit(tenantId, "ROLE_ASSIGNED", userRole.getId());
        return userRole;
    }

    @Transactional
    public void revoke(UUID tenantId, UUID userRoleId) {
        UserRole userRole = userRoleRepository.findByTenantIdAndId(tenantId, userRoleId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.RESOURCE_NOT_FOUND, "Role assignment not found: " + userRoleId));
        userRoleRepository.delete(userRole);
        audit(tenantId, "ROLE_REVOKED", userRoleId);
    }

    public List<UserRole> listUserRoles(UUID tenantId, UUID userId) {
        return userRoleRepository.findByTenantIdAndUserId(tenantId, userId);
    }

    private void audit(UUID tenantId, String action, UUID resourceId) {
        auditEventPublisher.publish(AuditEvent.of(
                tenantId,
                CurrentUserContext.getUserId(),
                action,
                "ROLE",
                resourceId.toString(),
                "SUCCESS",
                Map.of()
        ));
    }
}
