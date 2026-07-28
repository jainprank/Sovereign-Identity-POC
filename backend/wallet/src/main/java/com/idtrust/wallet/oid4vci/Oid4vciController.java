package com.idtrust.wallet.oid4vci;

import com.idtrust.common.tenant.TenantContext;
import com.idtrust.common.web.ApiErrorCode;
import com.idtrust.common.web.ApiException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Implements the OID4VCI protocol surface of EPIC 9. {@link #offer} is issuer-side and
 * authenticated like any other issuance operation; {@link #issuerMetadata}, {@link #token} and
 * {@link #credential} are the wallet-facing protocol endpoints a holder's wallet calls with no
 * Keycloak session, so they are {@code permitAll} in {@code SecurityConfig} - tenant scoping for
 * them still works because {@code TenantContextInterceptor} populates {@link TenantContext} from
 * the {@code X-Tenant-ID} header alone, regardless of authentication (see that class's Javadoc).
 * Callers of these three endpoints must therefore always send {@code X-Tenant-ID}.
 */
@RestController
@RequestMapping("/api/v1/oid4vci")
public class Oid4vciController {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String PRE_AUTHORIZED_CODE_GRANT = "urn:ietf:params:oauth:grant-type:pre-authorized_code";

    private final Oid4vciSessionService oid4vciSessionService;

    public Oid4vciController(Oid4vciSessionService oid4vciSessionService) {
        this.oid4vciSessionService = oid4vciSessionService;
    }

    @PostMapping("/offers")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN', 'ISSUER')")
    public ResponseEntity<Oid4vciDtos.OfferResponse> offer(@Valid @RequestBody Oid4vciDtos.CreateOfferRequest request) {
        Oid4vciSessionService.OfferResult result =
                oid4vciSessionService.createOffer(TenantContext.requireTenantId(), request.credentialId());
        return ResponseEntity.status(HttpStatus.CREATED).body(Oid4vciDtos.OfferResponse.from(result));
    }

    @GetMapping("/issuer-metadata")
    public Map<String, Object> issuerMetadata() {
        return oid4vciSessionService.issuerMetadata(TenantContext.requireTenantId());
    }

    @PostMapping("/token")
    public Oid4vciDtos.TokenResponse token(@Valid @RequestBody Oid4vciDtos.TokenRequest request) {
        if (!PRE_AUTHORIZED_CODE_GRANT.equals(request.grant_type())) {
            throw new ApiException(ApiErrorCode.INVALID_REQUEST, "Unsupported grant_type: " + request.grant_type());
        }
        Oid4vciSessionService.TokenResult result =
                oid4vciSessionService.issueToken(TenantContext.requireTenantId(), request.preAuthorizedCode());
        return Oid4vciDtos.TokenResponse.from(result);
    }

    @PostMapping("/credential")
    public Oid4vciDtos.CredentialResponse credential(@RequestHeader("Authorization") String authorization) {
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            throw new ApiException(ApiErrorCode.UNAUTHORIZED, "Missing or malformed Authorization header");
        }
        String accessToken = authorization.substring(BEARER_PREFIX.length());
        Oid4vciSessionService.CredentialResult result =
                oid4vciSessionService.redeemCredential(TenantContext.requireTenantId(), accessToken);
        return Oid4vciDtos.CredentialResponse.from(result);
    }
}
