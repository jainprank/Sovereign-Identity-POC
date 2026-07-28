package com.idtrust.credential.template;

import com.idtrust.common.tenant.TenantContext;
import com.idtrust.common.web.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Tenant-scoped CRUD API for reusable issuance presets (credential_templates). */
@RestController
@RequestMapping("/api/v1/credential-templates")
public class CredentialTemplateController {

    private final CredentialTemplateService templateService;

    public CredentialTemplateController(CredentialTemplateService templateService) {
        this.templateService = templateService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN', 'ISSUER')")
    public ResponseEntity<CredentialTemplateDtos.Response> create(
            @Valid @RequestBody CredentialTemplateDtos.CreateRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        CredentialTemplate template = templateService.create(tenantId, request.schemaId(), request.name(),
                request.description(), request.defaultClaims(), request.defaultExpiresInDays());
        return ResponseEntity.status(HttpStatus.CREATED).body(CredentialTemplateDtos.Response.from(template));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN', 'ISSUER')")
    public PageResponse<CredentialTemplateDtos.Response> list(@RequestParam(defaultValue = "0") int page,
                                                                 @RequestParam(defaultValue = "50") int size) {
        UUID tenantId = TenantContext.requireTenantId();
        return PageResponse.of(templateService.list(tenantId, PageRequest.of(page, Math.min(size, 200))),
                CredentialTemplateDtos.Response::from);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN', 'ISSUER')")
    public CredentialTemplateDtos.Response get(@PathVariable UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        return CredentialTemplateDtos.Response.from(templateService.get(tenantId, id));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN', 'ISSUER')")
    public CredentialTemplateDtos.Response update(@PathVariable UUID id,
                                                     @RequestBody CredentialTemplateDtos.UpdateRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        return CredentialTemplateDtos.Response.from(templateService.update(tenantId, id, request.description(),
                request.defaultClaims(), request.defaultExpiresInDays()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN', 'ISSUER')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        templateService.delete(tenantId, id);
        return ResponseEntity.noContent().build();
    }
}
