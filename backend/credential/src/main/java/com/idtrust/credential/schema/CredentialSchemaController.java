package com.idtrust.credential.schema;

import com.idtrust.common.tenant.TenantContext;
import com.idtrust.common.web.ETagSupport;
import com.idtrust.common.web.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Tenant-scoped API for credential schemas (EPIC 5). Read access is also granted to VERIFIER
 * (verifiers need to know a schema's claims to build verification policies against it - FR-VER-002),
 * but mutations are restricted to the roles that own schema governance per the seeded role
 * catalogue (PLATFORM_ADMIN, ORG_ADMIN, ISSUER).
 */
@RestController
@RequestMapping("/api/v1/credential-schemas")
public class CredentialSchemaController {

    private final CredentialSchemaService schemaService;

    public CredentialSchemaController(CredentialSchemaService schemaService) {
        this.schemaService = schemaService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN', 'ISSUER')")
    public ResponseEntity<CredentialSchemaDtos.Response> create(@Valid @RequestBody CredentialSchemaDtos.CreateRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        CredentialSchema schema = schemaService.create(tenantId, request.name(), request.version(), request.format(),
                new SchemaDefinition(request.claims()));
        return ResponseEntity.status(HttpStatus.CREATED).body(CredentialSchemaDtos.Response.from(schema));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN', 'ISSUER', 'VERIFIER')")
    public PageResponse<CredentialSchemaDtos.Response> search(@RequestParam(required = false) String name,
                                                                 @RequestParam(required = false) SchemaStatus status,
                                                                 @RequestParam(defaultValue = "0") int page,
                                                                 @RequestParam(defaultValue = "50") int size) {
        UUID tenantId = TenantContext.requireTenantId();
        return PageResponse.of(schemaService.search(tenantId, name, status, PageRequest.of(page, Math.min(size, 200))),
                CredentialSchemaDtos.Response::from);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN', 'ISSUER', 'VERIFIER')")
    public ResponseEntity<CredentialSchemaDtos.Response> get(@PathVariable UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        CredentialSchema schema = schemaService.get(tenantId, id);
        return ResponseEntity.ok()
                .eTag(ETagSupport.toHeaderValue(schema.getLockVersion()))
                .body(CredentialSchemaDtos.Response.from(schema));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN', 'ISSUER')")
    public ResponseEntity<CredentialSchemaDtos.Response> update(@PathVariable UUID id,
                                                                   @RequestHeader(value = "If-Match", required = false) String ifMatch,
                                                                   @RequestBody CredentialSchemaDtos.UpdateRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        ETagSupport.requireMatch(ifMatch, schemaService.get(tenantId, id).getLockVersion());
        SchemaDefinition definition = request.claims() != null ? new SchemaDefinition(request.claims()) : null;
        CredentialSchema schema = schemaService.update(tenantId, id, request.name(), definition);
        return ResponseEntity.ok()
                .eTag(ETagSupport.toHeaderValue(schema.getLockVersion()))
                .body(CredentialSchemaDtos.Response.from(schema));
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN', 'ISSUER')")
    public CredentialSchemaDtos.Response publish(@PathVariable UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        return CredentialSchemaDtos.Response.from(schemaService.publish(tenantId, id));
    }

    @PostMapping("/{id}/deprecate")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN', 'ISSUER')")
    public CredentialSchemaDtos.Response deprecate(@PathVariable UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        return CredentialSchemaDtos.Response.from(schemaService.deprecate(tenantId, id));
    }

    @PostMapping("/{id}/versions")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN', 'ISSUER')")
    public ResponseEntity<CredentialSchemaDtos.Response> createNewVersion(
            @PathVariable UUID id, @Valid @RequestBody CredentialSchemaDtos.NewVersionRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        SchemaDefinition definition = request.claims() != null ? new SchemaDefinition(request.claims()) : null;
        CredentialSchema schema = schemaService.createNewVersion(tenantId, id, request.version(), definition);
        return ResponseEntity.status(HttpStatus.CREATED).body(CredentialSchemaDtos.Response.from(schema));
    }
}
