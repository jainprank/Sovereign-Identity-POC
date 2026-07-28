package com.idtrust.kms.key;

import com.idtrust.common.tenant.TenantContext;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Key lifecycle API (FR-KMS-*). Restricted to org/platform administrators - keys are
 * infrastructure for DIDs/credential issuance, never a holder- or verifier-facing resource.
 * Signing is intentionally not exposed here (see {@link com.idtrust.kms.provider.KeyProvider}).
 */
@RestController
public class KeyController {

    private final KeyService keyService;

    public KeyController(KeyService keyService) {
        this.keyService = keyService;
    }

    @PostMapping("/api/v1/keys")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN')")
    public ResponseEntity<KeyDtos.Response> create(@Valid @RequestBody KeyDtos.CreateRequest request) {
        ManagedKey key = keyService.generate(TenantContext.requireTenantId(), request.keyType(), request.algorithm(), request.provider());
        return ResponseEntity.status(HttpStatus.CREATED).body(KeyDtos.Response.from(key));
    }

    @GetMapping("/api/v1/keys/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN')")
    public KeyDtos.Response get(@PathVariable UUID id) {
        return KeyDtos.Response.from(keyService.get(TenantContext.requireTenantId(), id));
    }

    @PostMapping("/api/v1/keys/{id}/rotate")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN')")
    public KeyDtos.Response rotate(@PathVariable UUID id) {
        return KeyDtos.Response.from(keyService.rotate(TenantContext.requireTenantId(), id));
    }

    @PostMapping("/api/v1/keys/{id}/deactivate")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN')")
    public KeyDtos.Response deactivate(@PathVariable UUID id) {
        return KeyDtos.Response.from(keyService.deactivate(TenantContext.requireTenantId(), id));
    }

    @DeleteMapping("/api/v1/keys/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        keyService.delete(TenantContext.requireTenantId(), id);
        return ResponseEntity.noContent().build();
    }
}
