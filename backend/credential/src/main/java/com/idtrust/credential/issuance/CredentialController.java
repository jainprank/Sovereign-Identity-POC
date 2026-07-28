package com.idtrust.credential.issuance;

import com.idtrust.common.tenant.TenantContext;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Tenant-scoped API for credential issuance (EPIC 6). Issuance and tracking are restricted to the
 * roles that own credential issuance per the seeded role catalogue (PLATFORM_ADMIN, ORG_ADMIN,
 * ISSUER) - unlike credential schemas, VERIFIER has no read access here (verifiers work with
 * presented credentials via E10, not the issuer's own issuance records).
 */
@RestController
@RequestMapping("/api/v1/credentials")
public class CredentialController {

    private final CredentialIssuanceService credentialIssuanceService;

    public CredentialController(CredentialIssuanceService credentialIssuanceService) {
        this.credentialIssuanceService = credentialIssuanceService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN', 'ISSUER')")
    public ResponseEntity<CredentialDtos.IssueResponse> issue(@Valid @RequestBody CredentialDtos.IssueRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        CredentialIssuanceService.IssuedCredential issued = credentialIssuanceService.issue(tenantId, request.schemaId(),
                request.issuerDid(), request.subjectDid(), request.claims(), request.expiresAt());
        return ResponseEntity.status(HttpStatus.CREATED).body(CredentialDtos.IssueResponse.from(issued));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN', 'ISSUER')")
    public List<CredentialDtos.Response> search(@RequestParam(required = false) String subjectDid,
                                                  @RequestParam(required = false) CredentialStatus status) {
        UUID tenantId = TenantContext.requireTenantId();
        return credentialIssuanceService.search(tenantId, subjectDid, status).stream()
                .map(CredentialDtos.Response::from).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN', 'ISSUER')")
    public CredentialDtos.Response get(@PathVariable UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        return CredentialDtos.Response.from(credentialIssuanceService.get(tenantId, id));
    }
}
