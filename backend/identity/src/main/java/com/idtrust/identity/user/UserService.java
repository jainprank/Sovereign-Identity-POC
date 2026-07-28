package com.idtrust.identity.user;

import com.idtrust.common.audit.AuditEvent;
import com.idtrust.common.audit.AuditEventPublisher;
import com.idtrust.common.tenant.CurrentUserContext;
import com.idtrust.common.web.ApiErrorCode;
import com.idtrust.common.web.ApiException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Implements FR-ORG-005 (User Lifecycle), FR-ORG-009 (Suspend User) and FR-ORG-010 (Deactivate
 * User). User creation itself happens as part of accepting an invitation (FR-ORG-007/008), see
 * the {@code invitation} package.
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final AuditEventPublisher auditEventPublisher;

    public UserService(UserRepository userRepository, AuditEventPublisher auditEventPublisher) {
        this.userRepository = userRepository;
        this.auditEventPublisher = auditEventPublisher;
    }

    @Transactional
    public AppUser create(UUID tenantId, String email, String displayName) {
        if (userRepository.existsByTenantIdAndEmail(tenantId, email)) {
            throw new ApiException(ApiErrorCode.RESOURCE_CONFLICT, "A user with this email already exists in the tenant: " + email);
        }
        AppUser user = new AppUser(tenantId, email, displayName);
        userRepository.save(user);
        audit(tenantId, "USER_CREATED", user.getId());
        return user;
    }

    public List<AppUser> listByTenant(UUID tenantId) {
        return userRepository.findByTenantId(tenantId);
    }

    public Page<AppUser> listByTenant(UUID tenantId, Pageable pageable) {
        return userRepository.findByTenantId(tenantId, pageable);
    }

    public AppUser get(UUID tenantId, UUID id) {
        return userRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new ApiException(ApiErrorCode.RESOURCE_NOT_FOUND, "User not found: " + id));
    }

    @Transactional
    public AppUser suspend(UUID tenantId, UUID id) {
        AppUser user = get(tenantId, id);
        user.suspend();
        audit(tenantId, "USER_SUSPENDED", user.getId());
        return user;
    }

    @Transactional
    public AppUser reactivate(UUID tenantId, UUID id) {
        AppUser user = get(tenantId, id);
        user.reactivate();
        audit(tenantId, "USER_REACTIVATED", user.getId());
        return user;
    }

    @Transactional
    public AppUser deactivate(UUID tenantId, UUID id) {
        AppUser user = get(tenantId, id);
        user.deactivate();
        audit(tenantId, "USER_DEACTIVATED", user.getId());
        return user;
    }

    private void audit(UUID tenantId, String action, UUID userId) {
        auditEventPublisher.publish(AuditEvent.of(
                tenantId,
                CurrentUserContext.getUserId(),
                action,
                "USER",
                userId.toString(),
                "SUCCESS",
                Map.of()
        ));
    }
}
