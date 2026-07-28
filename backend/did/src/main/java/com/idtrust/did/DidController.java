package com.idtrust.did;

import com.idtrust.common.tenant.TenantContext;
import com.idtrust.common.web.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/**
 * DID lifecycle API (FR-DID-*). Restricted to administrators/issuers - DIDs are infrastructure
 * a tenant provisions for itself (e.g. to become a credential issuer), not a resource end users
 * create directly. {@link #resolve} is the one public, unauthenticated exception - see
 * {@link DidService}.
 */
@RestController
public class DidController {

    private final DidService didService;

    public DidController(DidService didService) {
        this.didService = didService;
    }

    @PostMapping("/api/v1/dids")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN', 'ISSUER')")
    public ResponseEntity<DidDtos.Response> create(@Valid @RequestBody DidDtos.CreateRequest request) {
        Did did = didService.create(TenantContext.requireTenantId(), request.method(), request.domain(), request.path());
        return ResponseEntity.status(HttpStatus.CREATED).body(DidDtos.Response.from(did));
    }

    @GetMapping("/api/v1/dids")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN', 'ISSUER')")
    public PageResponse<DidDtos.Response> list(@RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "50") int size) {
        return PageResponse.of(didService.listByTenant(TenantContext.requireTenantId(), PageRequest.of(page, Math.min(size, 200))),
                DidDtos.Response::from);
    }

    @GetMapping("/api/v1/dids/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN', 'ISSUER')")
    public DidDtos.Response get(@PathVariable UUID id) {
        return DidDtos.Response.from(didService.get(TenantContext.requireTenantId(), id));
    }

    @GetMapping("/api/v1/dids/resolve")
    public Map<String, Object> resolve(@RequestParam String did) {
        return didService.resolve(did);
    }

    @PostMapping("/api/v1/dids/{id}/rotate")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN', 'ISSUER')")
    public DidDtos.Response rotate(@PathVariable UUID id) {
        return DidDtos.Response.from(didService.rotate(TenantContext.requireTenantId(), id));
    }

    @PostMapping("/api/v1/dids/{id}/deactivate")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN', 'ISSUER')")
    public DidDtos.Response deactivate(@PathVariable UUID id) {
        return DidDtos.Response.from(didService.deactivate(TenantContext.requireTenantId(), id));
    }
}
