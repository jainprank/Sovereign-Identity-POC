package com.idtrust.wallet;

import com.idtrust.common.tenant.TenantContext;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Tenant-scoped API for EPIC 8 (Digital Wallet): FR-WAL-001, 004 through 009. Open to HOLDER, the
 * seeded role a wallet's actual end user would carry, alongside the usual PLATFORM_ADMIN/ORG_ADMIN
 * who can operate wallets on a tenant's behalf. See {@link WalletService} for the known limitation
 * of {@link #list} returning every wallet in the tenant rather than only the caller's own.
 */
@RestController
@RequestMapping("/api/v1/wallets")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN', 'HOLDER')")
    public ResponseEntity<WalletDtos.Response> create(@Valid @RequestBody WalletDtos.CreateRequest request) {
        Wallet wallet = walletService.create(TenantContext.requireTenantId(), request.label());
        return ResponseEntity.status(HttpStatus.CREATED).body(WalletDtos.Response.from(wallet));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN', 'HOLDER')")
    public List<WalletDtos.Response> list() {
        return walletService.list(TenantContext.requireTenantId()).stream().map(WalletDtos.Response::from).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN', 'HOLDER')")
    public WalletDtos.Response get(@PathVariable UUID id) {
        return WalletDtos.Response.from(walletService.get(TenantContext.requireTenantId(), id));
    }

    @GetMapping("/{id}/credentials")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN', 'HOLDER')")
    public List<WalletDtos.WalletCredentialResponse> listCredentials(@PathVariable UUID id,
                                                                       @RequestParam(required = false) String category) {
        return walletService.listCredentials(TenantContext.requireTenantId(), id, category).stream()
                .map(WalletDtos.WalletCredentialResponse::from).toList();
    }

    @GetMapping("/{id}/credentials/{credentialId}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN', 'HOLDER')")
    public WalletDtos.WalletCredentialResponse getCredential(@PathVariable UUID id, @PathVariable UUID credentialId) {
        return WalletDtos.WalletCredentialResponse.from(
                walletService.getCredential(TenantContext.requireTenantId(), id, credentialId));
    }

    @PutMapping("/{id}/credentials/{credentialId}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN', 'HOLDER')")
    public WalletDtos.WalletCredentialResponse recategorize(@PathVariable UUID id, @PathVariable UUID credentialId,
                                                              @Valid @RequestBody WalletDtos.RecategorizeRequest request) {
        return WalletDtos.WalletCredentialResponse.from(
                walletService.recategorize(TenantContext.requireTenantId(), id, credentialId, request.category()));
    }

    @DeleteMapping("/{id}/credentials/{credentialId}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN', 'HOLDER')")
    public ResponseEntity<Void> deleteCredential(@PathVariable UUID id, @PathVariable UUID credentialId) {
        walletService.deleteCredential(TenantContext.requireTenantId(), id, credentialId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/claim")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN', 'HOLDER')")
    public ResponseEntity<WalletDtos.WalletCredentialResponse> claim(@PathVariable UUID id,
                                                                      @Valid @RequestBody WalletDtos.ClaimRequest request) {
        WalletService.WalletCredentialView view =
                walletService.claim(TenantContext.requireTenantId(), id, request.preAuthorizedCode());
        return ResponseEntity.status(HttpStatus.CREATED).body(WalletDtos.WalletCredentialResponse.from(view));
    }

    @PostMapping("/{id}/credentials/{credentialId}/present")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN', 'HOLDER')")
    public ResponseEntity<WalletDtos.PresentationResponse> present(@PathVariable UUID id, @PathVariable UUID credentialId,
                                                                     @Valid @RequestBody WalletDtos.PresentRequest request) {
        var presentation = walletService.presentCredential(
                TenantContext.requireTenantId(), id, credentialId, request.audience(), request.nonce(),
                request.disclosedClaims());
        return ResponseEntity.status(HttpStatus.CREATED).body(WalletDtos.PresentationResponse.from(presentation));
    }

    @GetMapping("/{id}/presentations")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN', 'HOLDER')")
    public List<WalletDtos.PresentationResponse> listPresentations(@PathVariable UUID id) {
        return walletService.listPresentations(TenantContext.requireTenantId(), id).stream()
                .map(WalletDtos.PresentationResponse::from).toList();
    }
}
