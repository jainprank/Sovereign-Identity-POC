package com.idtrust.identity.organization;

import com.idtrust.common.audit.AuditEvent;
import com.idtrust.common.audit.AuditEventPublisher;
import com.idtrust.common.tenant.CurrentUserContext;
import com.idtrust.common.web.ApiErrorCode;
import com.idtrust.common.web.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * Implements FR-ORG-001 (Create Organization), FR-ORG-002 (Organization Profile) and FR-ORG-003
 * (Deactivate Organization).
 */
@Service
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final AuditEventPublisher auditEventPublisher;

    public OrganizationService(OrganizationRepository organizationRepository, AuditEventPublisher auditEventPublisher) {
        this.organizationRepository = organizationRepository;
        this.auditEventPublisher = auditEventPublisher;
    }

    @Transactional
    public Organization create(String name, String description, String contactEmail) {
        Organization organization = new Organization(name, description, contactEmail);
        organizationRepository.save(organization);
        audit("ORGANIZATION_CREATED", organization.getId());
        return organization;
    }

    public Organization get(UUID id) {
        return organizationRepository.findById(id)
                .orElseThrow(() -> new ApiException(ApiErrorCode.RESOURCE_NOT_FOUND, "Organization not found: " + id));
    }

    @Transactional
    public Organization update(UUID id, String name, String description, String logoUrl, String website, String contactEmail) {
        Organization organization = get(id);
        organization.updateProfile(name, description, logoUrl, website, contactEmail);
        audit("ORGANIZATION_UPDATED", organization.getId());
        return organization;
    }

    @Transactional
    public Organization deactivate(UUID id) {
        Organization organization = get(id);
        organization.deactivate();
        audit("ORGANIZATION_DEACTIVATED", organization.getId());
        return organization;
    }

    private void audit(String action, UUID organizationId) {
        auditEventPublisher.publish(AuditEvent.of(
                null,
                CurrentUserContext.getUserId(),
                action,
                "ORGANIZATION",
                organizationId.toString(),
                "SUCCESS",
                Map.of()
        ));
    }
}
