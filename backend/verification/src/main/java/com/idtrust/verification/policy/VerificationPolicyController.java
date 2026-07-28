package com.idtrust.verification.policy;

import com.idtrust.common.tenant.TenantContext;
import com.idtrust.common.web.ETagSupport;
import jakarta.validation.Valid;
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

import java.util.List;
import java.util.UUID;

/**
 * Tenant-scoped API for verification policies (EPIC 12). Mutations are restricted to
 * PLATFORM_ADMIN/ORG_ADMIN/VERIFIER - the role that owns verification governance, mirroring
 * {@code credential.schema.CredentialSchemaController}'s ISSUER-owns-schemas precedent.
 */
@RestController
@RequestMapping("/api/v1/verification-policies")
public class VerificationPolicyController {

    private final VerificationPolicyService policyService;

    public VerificationPolicyController(VerificationPolicyService policyService) {
        this.policyService = policyService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN', 'VERIFIER')")
    public ResponseEntity<PolicyDtos.Response> create(@Valid @RequestBody PolicyDtos.CreateRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        VerificationPolicy policy = policyService.create(tenantId, request.name(), request.rules().toDefinition());
        return ResponseEntity.status(HttpStatus.CREATED).body(PolicyDtos.Response.from(policy));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN', 'VERIFIER')")
    public List<PolicyDtos.Response> search(@RequestParam(required = false) String name,
                                             @RequestParam(required = false) PolicyStatus status) {
        UUID tenantId = TenantContext.requireTenantId();
        return policyService.search(tenantId, name, status).stream().map(PolicyDtos.Response::from).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN', 'VERIFIER')")
    public ResponseEntity<PolicyDtos.Response> get(@PathVariable UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        VerificationPolicy policy = policyService.get(tenantId, id);
        return ResponseEntity.ok()
                .eTag(ETagSupport.toHeaderValue(policy.getLockVersion()))
                .body(PolicyDtos.Response.from(policy));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN', 'VERIFIER')")
    public ResponseEntity<PolicyDtos.Response> update(@PathVariable UUID id,
                                                         @RequestHeader(value = "If-Match", required = false) String ifMatch,
                                                         @RequestBody PolicyDtos.UpdateRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        ETagSupport.requireMatch(ifMatch, policyService.get(tenantId, id).getLockVersion());
        PolicyDefinition definition = request.rules() != null ? request.rules().toDefinition() : null;
        VerificationPolicy policy = policyService.update(tenantId, id, definition);
        return ResponseEntity.ok()
                .eTag(ETagSupport.toHeaderValue(policy.getLockVersion()))
                .body(PolicyDtos.Response.from(policy));
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN', 'VERIFIER')")
    public PolicyDtos.Response activate(@PathVariable UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        return PolicyDtos.Response.from(policyService.activate(tenantId, id));
    }

    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN', 'VERIFIER')")
    public PolicyDtos.Response deactivate(@PathVariable UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        return PolicyDtos.Response.from(policyService.deactivate(tenantId, id));
    }

    @PostMapping("/{id}/versions")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN', 'VERIFIER')")
    public ResponseEntity<PolicyDtos.Response> createNewVersion(@PathVariable UUID id,
                                                                  @RequestBody PolicyDtos.NewVersionRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        PolicyDefinition definition = request.rules() != null ? request.rules().toDefinition() : null;
        VerificationPolicy policy = policyService.createNewVersion(tenantId, id, definition);
        return ResponseEntity.status(HttpStatus.CREATED).body(PolicyDtos.Response.from(policy));
    }
}
