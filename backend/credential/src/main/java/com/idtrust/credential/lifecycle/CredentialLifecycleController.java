package com.idtrust.credential.lifecycle;

import com.idtrust.common.tenant.TenantContext;
import com.idtrust.credential.issuance.CredentialDtos;
import jakarta.validation.Valid;
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
 * Tenant-scoped API for credential lifecycle management (EPIC 7). Mutating operations (revoke,
 * suspend, reinstate, renew, reissue) are restricted the same way as issuance itself
 * (PLATFORM_ADMIN, ORG_ADMIN, ISSUER). {@code status} and {@code status-list} (FR-CRED-014,
 * FR-CRED-011) additionally allow VERIFIER, since a verifier needs to check a credential's current
 * status when validating a presentation - ahead of E10, this is exposed here so the capability
 * already exists once verification is built.
 */
@RestController
@RequestMapping("/api/v1/credentials")
public class CredentialLifecycleController {

    private final CredentialLifecycleService credentialLifecycleService;

    public CredentialLifecycleController(CredentialLifecycleService credentialLifecycleService) {
        this.credentialLifecycleService = credentialLifecycleService;
    }

    @PostMapping("/{id}/revoke")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN', 'ISSUER')")
    public CredentialDtos.Response revoke(@PathVariable UUID id,
                                           @RequestBody(required = false) CredentialLifecycleDtos.RevokeRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        String reason = request != null ? request.reason() : null;
        return CredentialDtos.Response.from(credentialLifecycleService.revoke(tenantId, id, reason));
    }

    @PostMapping("/{id}/suspend")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN', 'ISSUER')")
    public CredentialDtos.Response suspend(@PathVariable UUID id,
                                            @RequestBody(required = false) CredentialLifecycleDtos.SuspendRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        String reason = request != null ? request.reason() : null;
        return CredentialDtos.Response.from(credentialLifecycleService.suspend(tenantId, id, reason));
    }

    @PostMapping("/{id}/reinstate")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN', 'ISSUER')")
    public CredentialDtos.Response reinstate(@PathVariable UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        return CredentialDtos.Response.from(credentialLifecycleService.reinstate(tenantId, id));
    }

    @PostMapping("/{id}/renew")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN', 'ISSUER')")
    public CredentialDtos.IssueResponse renew(@PathVariable UUID id,
                                               @RequestBody(required = false) CredentialLifecycleDtos.RenewRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        var newExpiresAt = request != null ? request.expiresAt() : null;
        return CredentialDtos.IssueResponse.from(credentialLifecycleService.renew(tenantId, id, newExpiresAt));
    }

    @PostMapping("/{id}/reissue")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN', 'ISSUER')")
    public CredentialDtos.IssueResponse reissue(@PathVariable UUID id,
                                                 @Valid @RequestBody(required = false) CredentialLifecycleDtos.ReissueRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        var claims = request != null ? request.claims() : null;
        var newExpiresAt = request != null ? request.expiresAt() : null;
        return CredentialDtos.IssueResponse.from(credentialLifecycleService.reissue(tenantId, id, claims, newExpiresAt));
    }

    @GetMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN', 'ISSUER', 'VERIFIER')")
    public CredentialLifecycleDtos.StatusResponse status(@PathVariable UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        return new CredentialLifecycleDtos.StatusResponse(id, credentialLifecycleService.checkStatus(tenantId, id));
    }

    @GetMapping("/status-list")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN', 'ISSUER', 'VERIFIER')")
    public List<CredentialLifecycleService.StatusListEntry> statusList(@RequestParam(required = false) UUID schemaId) {
        UUID tenantId = TenantContext.requireTenantId();
        return credentialLifecycleService.statusList(tenantId, schemaId);
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN', 'ISSUER')")
    public List<CredentialLifecycleDtos.HistoryEntryResponse> history(@PathVariable UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        return credentialLifecycleService.history(tenantId, id).stream()
                .map(CredentialLifecycleDtos.HistoryEntryResponse::from)
                .toList();
    }
}
