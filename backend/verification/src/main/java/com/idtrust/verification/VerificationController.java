package com.idtrust.verification;

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
 * Tenant-scoped API for EPIC 10/11 (Verification &amp; OID4VP). {@link #create}, {@link #search}
 * and {@link #get} are issuer/verifier-side and authenticated like any other verification
 * operation; {@link #present} is the wallet-facing OID4VP protocol endpoint a holder's wallet
 * calls with no Keycloak session, so it is {@code permitAll} in {@code identity.security.
 * SecurityConfig} - tenant scoping still works because {@code TenantContextInterceptor} populates
 * {@code TenantContext} from the {@code X-Tenant-ID} header alone, regardless of authentication
 * (see {@code wallet.oid4vci.Oid4vciController}'s Javadoc for the established precedent). Callers
 * of {@link #present} must therefore always send {@code X-Tenant-ID} set to the verifier's tenant.
 */
@RestController
@RequestMapping("/api/v1/verification-requests")
public class VerificationController {

    private final VerificationService verificationService;

    public VerificationController(VerificationService verificationService) {
        this.verificationService = verificationService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN', 'VERIFIER')")
    public ResponseEntity<VerificationDtos.RequestResponse> create(@Valid @RequestBody VerificationDtos.CreateRequest request) {
        VerificationService.RequestResult result = verificationService.createRequest(
                TenantContext.requireTenantId(), request.policyId(), request.callbackUrl());
        return ResponseEntity.status(HttpStatus.CREATED).body(VerificationDtos.RequestResponse.from(result));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN', 'VERIFIER')")
    public List<VerificationDtos.ResultResponse> search(@RequestParam(required = false) VerificationStatus status) {
        return verificationService.list(TenantContext.requireTenantId(), status).stream()
                .map(VerificationDtos.ResultResponse::from).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN', 'VERIFIER')")
    public VerificationDtos.ResultResponse get(@PathVariable UUID id) {
        return VerificationDtos.ResultResponse.from(verificationService.get(TenantContext.requireTenantId(), id));
    }

    @PostMapping("/{id}/present")
    public VerificationDtos.ResultResponse present(@PathVariable UUID id,
                                                     @Valid @RequestBody VerificationDtos.PresentRequest request) {
        VerificationRequest result = verificationService.presentAndVerify(
                TenantContext.requireTenantId(), id, request.vpJwt());
        return VerificationDtos.ResultResponse.from(result);
    }
}
