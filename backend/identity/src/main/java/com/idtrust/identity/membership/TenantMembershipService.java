package com.idtrust.identity.membership;

import com.idtrust.common.audit.AuditEvent;
import com.idtrust.common.audit.AuditEventPublisher;
import com.idtrust.common.tenant.CurrentUserContext;
import com.idtrust.common.web.ApiErrorCode;
import com.idtrust.common.web.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class TenantMembershipService {

    private final TenantMembershipRepository membershipRepository;
    private final AuditEventPublisher auditEventPublisher;

    public TenantMembershipService(TenantMembershipRepository membershipRepository, AuditEventPublisher auditEventPublisher) {
        this.membershipRepository = membershipRepository;
        this.auditEventPublisher = auditEventPublisher;
    }

    @Transactional
    public TenantMembership create(UUID tenantId, UUID userId, MembershipRole role) {
        if (membershipRepository.findByTenantIdAndUserId(tenantId, userId).isPresent()) {
            throw new ApiException(ApiErrorCode.RESOURCE_CONFLICT, "User " + userId + " already has a membership in tenant " + tenantId);
        }
        TenantMembership membership = new TenantMembership(tenantId, userId, role);
        membershipRepository.save(membership);
        audit(tenantId, "TENANT_MEMBERSHIP_CREATED", membership.getId());
        return membership;
    }

    public List<TenantMembership> list(UUID tenantId) {
        return membershipRepository.findByTenantId(tenantId);
    }

    public TenantMembership get(UUID tenantId, UUID id) {
        return membershipRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new ApiException(ApiErrorCode.RESOURCE_NOT_FOUND, "Tenant membership not found: " + id));
    }

    @Transactional
    public TenantMembership changeRole(UUID tenantId, UUID id, MembershipRole role) {
        TenantMembership membership = get(tenantId, id);
        membership.changeRole(role);
        audit(tenantId, "TENANT_MEMBERSHIP_ROLE_CHANGED", membership.getId());
        return membership;
    }

    @Transactional
    public TenantMembership revoke(UUID tenantId, UUID id) {
        TenantMembership membership = get(tenantId, id);
        membership.revoke();
        audit(tenantId, "TENANT_MEMBERSHIP_REVOKED", membership.getId());
        return membership;
    }

    private void audit(UUID tenantId, String action, UUID membershipId) {
        auditEventPublisher.publish(AuditEvent.of(
                tenantId,
                CurrentUserContext.getUserId(),
                action,
                "TENANT_MEMBERSHIP",
                membershipId.toString(),
                "SUCCESS",
                Map.of()
        ));
    }
}
