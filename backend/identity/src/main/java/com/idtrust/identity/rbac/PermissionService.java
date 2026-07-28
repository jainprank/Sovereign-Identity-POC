package com.idtrust.identity.rbac;

import org.springframework.stereotype.Service;

import java.util.List;

/** Read-only access to the global permission catalogue (seeded via SQL migration). */
@Service
public class PermissionService {

    private final PermissionRepository permissionRepository;

    public PermissionService(PermissionRepository permissionRepository) {
        this.permissionRepository = permissionRepository;
    }

    public List<Permission> listAll() {
        return permissionRepository.findAll();
    }
}
