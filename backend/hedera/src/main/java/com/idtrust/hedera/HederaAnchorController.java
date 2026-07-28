package com.idtrust.hedera;

import com.idtrust.common.tenant.TenantContext;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Tenant-scoped API over previously recorded {@link HederaAnchor}s (FR-HED-009/010/012). Read
 * access mirrors {@code TrustRegistryController}'s VERIFIER-reads precedent - anchoring status is
 * evidence any tenant admin or verifier may need to inspect. {@link #retry} is restricted to
 * PLATFORM_ADMIN/ORG_ADMIN like every other remediation-style mutation in this codebase.
 *
 * <p>There is deliberately no anchoring-trigger endpoint here - anchors are created only as a
 * side effect of {@code trust}/{@code did}/{@code credential} operations via
 * {@code HederaAnchorPublisher}, never directly by a client.
 */
@RestController
@RequestMapping("/api/v1/hedera/anchors")
public class HederaAnchorController {

    private final HederaAnchorService anchorService;

    public HederaAnchorController(HederaAnchorService anchorService) {
        this.anchorService = anchorService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN', 'VERIFIER')")
    public List<HederaDtos.AnchorResponse> listForResource(@RequestParam @NotBlank String resourceType,
                                                            @RequestParam UUID resourceId) {
        return anchorService.listForResource(TenantContext.requireTenantId(), resourceType, resourceId).stream()
                .map(HederaDtos.AnchorResponse::from).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN', 'VERIFIER')")
    public HederaDtos.AnchorResponse get(@PathVariable UUID id) {
        return HederaDtos.AnchorResponse.from(anchorService.get(TenantContext.requireTenantId(), id));
    }

    @PostMapping("/{id}/retry")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN')")
    public HederaDtos.AnchorResponse retry(@PathVariable UUID id) {
        return HederaDtos.AnchorResponse.from(anchorService.retry(TenantContext.requireTenantId(), id));
    }

    @GetMapping("/{id}/verify")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN', 'VERIFIER')")
    public HederaDtos.VerifyResponse verify(@PathVariable UUID id) {
        boolean verified = anchorService.verify(TenantContext.requireTenantId(), id);
        return new HederaDtos.VerifyResponse(id, verified);
    }
}
