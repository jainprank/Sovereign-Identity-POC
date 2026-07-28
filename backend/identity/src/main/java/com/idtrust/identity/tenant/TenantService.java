package com.idtrust.identity.tenant;

import com.idtrust.common.audit.AuditEvent;
import com.idtrust.common.audit.AuditEventPublisher;
import com.idtrust.common.tenant.CurrentUserContext;
import com.idtrust.common.web.ApiErrorCode;
import com.idtrust.common.web.ApiException;
import com.idtrust.identity.organization.Organization;
import com.idtrust.identity.organization.OrganizationService;
import com.idtrust.identity.organization.OrganizationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Implements FR-ORG-004 (Create Tenant), FR-ORG-005 (Update Tenant), FR-ORG-006 (Suspend Tenant)
 * and FR-ORG-014 (Configure Tenant Settings).
 */
@Service
public class TenantService {

    private final TenantRepository tenantRepository;
    private final OrganizationService organizationService;
    private final AuditEventPublisher auditEventPublisher;

    public TenantService(TenantRepository tenantRepository, OrganizationService organizationService,
                          AuditEventPublisher auditEventPublisher) {
        this.tenantRepository = tenantRepository;
        this.organizationService = organizationService;
        this.auditEventPublisher = auditEventPublisher;
    }

    @Transactional
    public Tenant create(UUID organizationId, String name) {
        Organization organization = organizationService.get(organizationId);
        if (organization.getStatus() != OrganizationStatus.ACTIVE) {
            throw new ApiException(ApiErrorCode.INVALID_REQUEST, "Cannot create a tenant under an inactive organization");
        }
        Tenant tenant = new Tenant(organizationId, name);
        tenantRepository.save(tenant);
        audit("TENANT_CREATED", tenant.getId());
        return tenant;
    }

    public Tenant get(UUID id) {
        return tenantRepository.findById(id)
                .orElseThrow(() -> new ApiException(ApiErrorCode.RESOURCE_NOT_FOUND, "Tenant not found: " + id));
    }

    public List<Tenant> listByOrganization(UUID organizationId) {
        return tenantRepository.findByOrganizationId(organizationId);
    }

    public Page<Tenant> listByOrganization(UUID organizationId, Pageable pageable) {
        return tenantRepository.findByOrganizationId(organizationId, pageable);
    }

    public Page<Tenant> listAll(Pageable pageable) {
        return tenantRepository.findAll(pageable);
    }

    @Transactional
    public Tenant update(UUID id, String name) {
        Tenant tenant = get(id);
        tenant.rename(name);
        audit("TENANT_UPDATED", tenant.getId());
        return tenant;
    }

    @Transactional
    public Tenant updateConfiguration(UUID id, Map<String, Object> configuration) {
        Tenant tenant = get(id);
        tenant.updateConfiguration(configuration);
        audit("TENANT_CONFIGURATION_UPDATED", tenant.getId());
        return tenant;
    }

    @Transactional
    public Tenant suspend(UUID id) {
        Tenant tenant = get(id);
        tenant.suspend();
        audit("TENANT_SUSPENDED", tenant.getId());
        return tenant;
    }

    @Transactional
    public Tenant reactivate(UUID id) {
        Tenant tenant = get(id);
        tenant.reactivate();
        audit("TENANT_REACTIVATED", tenant.getId());
        return tenant;
    }

    private void audit(String action, UUID tenantId) {
        auditEventPublisher.publish(AuditEvent.of(
                tenantId,
                CurrentUserContext.getUserId(),
                action,
                "TENANT",
                tenantId.toString(),
                "SUCCESS",
                Map.of()
        ));
    }
}
