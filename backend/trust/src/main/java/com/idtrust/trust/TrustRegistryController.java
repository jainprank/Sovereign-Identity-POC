package com.idtrust.trust;

import com.idtrust.common.tenant.TenantContext;
import com.idtrust.common.web.ApiErrorCode;
import com.idtrust.common.web.ApiException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Tenant-scoped API for the Trust Registry (EPIC 13, FR-TRUST-*). Mutations are restricted to
 * PLATFORM_ADMIN/ORG_ADMIN - trust governance is an administrative function per the seeded
 * ORG_ADMIN role description ("...issuers, verifiers and policies"). VERIFIER gets read-only
 * access (including the {@link #lookup} endpoint), mirroring
 * {@code credential.schema.CredentialSchemaController}'s VERIFIER-reads-but-doesn't-mutate
 * precedent from E06 - verifiers need visibility into who is trusted to build policies, but trust
 * governance itself belongs to tenant admins.
 */
@RestController
@RequestMapping("/api/v1/trust-registries")
public class TrustRegistryController {

    private final TrustRegistryService trustRegistryService;

    public TrustRegistryController(TrustRegistryService trustRegistryService) {
        this.trustRegistryService = trustRegistryService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN')")
    public ResponseEntity<TrustDtos.RegistryResponse> create(@Valid @RequestBody TrustDtos.CreateRegistryRequest request) {
        TrustRegistry registry = trustRegistryService.create(TenantContext.requireTenantId(), request.name(), request.framework());
        return ResponseEntity.status(HttpStatus.CREATED).body(TrustDtos.RegistryResponse.from(registry));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN', 'VERIFIER')")
    public List<TrustDtos.RegistryResponse> list() {
        return trustRegistryService.list(TenantContext.requireTenantId()).stream()
                .map(TrustDtos.RegistryResponse::from).toList();
    }

    @GetMapping("/lookup")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN', 'VERIFIER')")
    public List<TrustDtos.IssuerResponse> lookup(@RequestParam String issuerDid) {
        return trustRegistryService.lookup(TenantContext.requireTenantId(), issuerDid).stream()
                .map(TrustDtos.IssuerResponse::from).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN', 'VERIFIER')")
    public TrustDtos.RegistryResponse get(@PathVariable UUID id) {
        return TrustDtos.RegistryResponse.from(trustRegistryService.get(TenantContext.requireTenantId(), id));
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN')")
    public TrustDtos.RegistryResponse activate(@PathVariable UUID id) {
        return TrustDtos.RegistryResponse.from(trustRegistryService.activate(TenantContext.requireTenantId(), id));
    }

    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN')")
    public TrustDtos.RegistryResponse deactivate(@PathVariable UUID id) {
        return TrustDtos.RegistryResponse.from(trustRegistryService.deactivate(TenantContext.requireTenantId(), id));
    }

    @PostMapping("/{id}/issuers")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN')")
    public ResponseEntity<TrustDtos.IssuerResponse> registerIssuer(@PathVariable UUID id,
                                                                     @Valid @RequestBody TrustDtos.RegisterIssuerRequest request) {
        TrustedIssuer issuer = trustRegistryService.registerIssuer(TenantContext.requireTenantId(), id,
                request.issuerDid(), request.organization(), request.trustedCredentialTypes());
        return ResponseEntity.status(HttpStatus.CREATED).body(TrustDtos.IssuerResponse.from(issuer));
    }

    @GetMapping("/{id}/issuers")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN', 'VERIFIER')")
    public List<TrustDtos.IssuerResponse> listIssuers(@PathVariable UUID id) {
        return trustRegistryService.listIssuers(TenantContext.requireTenantId(), id).stream()
                .map(TrustDtos.IssuerResponse::from).toList();
    }

    @GetMapping("/{id}/issuers/{issuerId}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN', 'VERIFIER')")
    public TrustDtos.IssuerResponse getIssuer(@PathVariable UUID id, @PathVariable UUID issuerId) {
        return TrustDtos.IssuerResponse.from(trustRegistryService.getIssuer(TenantContext.requireTenantId(), id, issuerId));
    }

    @PatchMapping("/{id}/issuers/{issuerId}/status")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN')")
    public TrustDtos.IssuerResponse updateStatus(@PathVariable UUID id, @PathVariable UUID issuerId,
                                                   @Valid @RequestBody TrustDtos.UpdateStatusRequest request) {
        TrustStatus status = parseStatus(request.status());
        TrustedIssuer issuer = trustRegistryService.updateStatus(TenantContext.requireTenantId(), id, issuerId, status);
        return TrustDtos.IssuerResponse.from(issuer);
    }

    @PostMapping("/{id}/issuers/{issuerId}/revoke")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN')")
    public TrustDtos.IssuerResponse revoke(@PathVariable UUID id, @PathVariable UUID issuerId) {
        TrustedIssuer issuer = trustRegistryService.revokeTrust(TenantContext.requireTenantId(), id, issuerId);
        return TrustDtos.IssuerResponse.from(issuer);
    }

    private TrustStatus parseStatus(String status) {
        try {
            return TrustStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ApiException(ApiErrorCode.INVALID_REQUEST, "Unknown trust status: " + status);
        }
    }
}
