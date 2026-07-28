package com.idtrust.credential.template;

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

@Service
public class CredentialTemplateService {

    private final CredentialTemplateRepository templateRepository;
    private final AuditEventPublisher auditEventPublisher;

    public CredentialTemplateService(CredentialTemplateRepository templateRepository,
                                      AuditEventPublisher auditEventPublisher) {
        this.templateRepository = templateRepository;
        this.auditEventPublisher = auditEventPublisher;
    }

    @Transactional
    public CredentialTemplate create(UUID tenantId, UUID schemaId, String name, String description,
                                      Map<String, Object> defaultClaims, Integer defaultExpiresInDays) {
        templateRepository.findByTenantIdAndName(tenantId, name).ifPresent(existing -> {
            throw new ApiException(ApiErrorCode.RESOURCE_CONFLICT,
                    "A credential template named '" + name + "' already exists");
        });
        CredentialTemplate template = new CredentialTemplate(tenantId, schemaId, name, description,
                defaultClaims, defaultExpiresInDays);
        templateRepository.save(template);
        audit(tenantId, "CREDENTIAL_TEMPLATE_CREATED", template.getId());
        return template;
    }

    public CredentialTemplate get(UUID tenantId, UUID id) {
        return templateRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new ApiException(ApiErrorCode.RESOURCE_NOT_FOUND, "Credential template not found: " + id));
    }

    public List<CredentialTemplate> list(UUID tenantId) {
        return templateRepository.findByTenantId(tenantId);
    }

    public Page<CredentialTemplate> list(UUID tenantId, Pageable pageable) {
        return templateRepository.findByTenantId(tenantId, pageable);
    }

    @Transactional
    public CredentialTemplate update(UUID tenantId, UUID id, String description, Map<String, Object> defaultClaims,
                                      Integer defaultExpiresInDays) {
        CredentialTemplate template = get(tenantId, id);
        template.update(description, defaultClaims, defaultExpiresInDays);
        audit(tenantId, "CREDENTIAL_TEMPLATE_UPDATED", template.getId());
        return template;
    }

    @Transactional
    public void delete(UUID tenantId, UUID id) {
        CredentialTemplate template = get(tenantId, id);
        templateRepository.delete(template);
        audit(tenantId, "CREDENTIAL_TEMPLATE_DELETED", id);
    }

    private void audit(UUID tenantId, String action, UUID templateId) {
        auditEventPublisher.publish(AuditEvent.of(
                tenantId,
                CurrentUserContext.getUserId(),
                action,
                "CREDENTIAL_TEMPLATE",
                templateId.toString(),
                "SUCCESS",
                Map.of()
        ));
    }
}
