package com.idtrust.identity.serviceaccount;

import com.idtrust.common.audit.AuditEvent;
import com.idtrust.common.audit.AuditEventPublisher;
import com.idtrust.common.tenant.CurrentUserContext;
import com.idtrust.common.web.ApiErrorCode;
import com.idtrust.common.web.ApiException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
public class ServiceAccountService {

    private final ServiceAccountRepository serviceAccountRepository;
    private final AuditEventPublisher auditEventPublisher;

    public ServiceAccountService(ServiceAccountRepository serviceAccountRepository, AuditEventPublisher auditEventPublisher) {
        this.serviceAccountRepository = serviceAccountRepository;
        this.auditEventPublisher = auditEventPublisher;
    }

    @Transactional
    public ServiceAccount create(UUID tenantId, String name, String description) {
        ServiceAccount serviceAccount = new ServiceAccount(tenantId, name, description);
        serviceAccountRepository.save(serviceAccount);
        audit(tenantId, "SERVICE_ACCOUNT_CREATED", serviceAccount.getId());
        return serviceAccount;
    }

    public Page<ServiceAccount> list(UUID tenantId, Pageable pageable) {
        return serviceAccountRepository.findByTenantId(tenantId, pageable);
    }

    public ServiceAccount get(UUID tenantId, UUID id) {
        return serviceAccountRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new ApiException(ApiErrorCode.RESOURCE_NOT_FOUND, "Service account not found: " + id));
    }

    @Transactional
    public ServiceAccount suspend(UUID tenantId, UUID id) {
        ServiceAccount serviceAccount = get(tenantId, id);
        serviceAccount.suspend();
        audit(tenantId, "SERVICE_ACCOUNT_SUSPENDED", serviceAccount.getId());
        return serviceAccount;
    }

    @Transactional
    public ServiceAccount reactivate(UUID tenantId, UUID id) {
        ServiceAccount serviceAccount = get(tenantId, id);
        serviceAccount.reactivate();
        audit(tenantId, "SERVICE_ACCOUNT_REACTIVATED", serviceAccount.getId());
        return serviceAccount;
    }

    @Transactional
    public ServiceAccount revoke(UUID tenantId, UUID id) {
        ServiceAccount serviceAccount = get(tenantId, id);
        serviceAccount.revoke();
        audit(tenantId, "SERVICE_ACCOUNT_REVOKED", serviceAccount.getId());
        return serviceAccount;
    }

    private void audit(UUID tenantId, String action, UUID serviceAccountId) {
        auditEventPublisher.publish(AuditEvent.of(
                tenantId,
                CurrentUserContext.getUserId(),
                action,
                "SERVICE_ACCOUNT",
                serviceAccountId.toString(),
                "SUCCESS",
                Map.of()
        ));
    }
}
