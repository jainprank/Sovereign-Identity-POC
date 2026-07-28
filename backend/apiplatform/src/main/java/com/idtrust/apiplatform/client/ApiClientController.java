package com.idtrust.apiplatform.client;

import com.idtrust.common.tenant.TenantContext;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Tenant-scoped management of {@link ApiClient}s (FR-API-001/002). Restricted to
 * PLATFORM_ADMIN/ORG_ADMIN, mirroring every other admin-mutation-style controller in this
 * codebase (e.g. {@code identity.user.UserController}'s invitation endpoints) - issuing a
 * machine credential is a privileged operation.
 */
@RestController
@RequestMapping("/api/v1/api-clients")
@PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN')")
public class ApiClientController {

    private final ApiClientService apiClientService;

    public ApiClientController(ApiClientService apiClientService) {
        this.apiClientService = apiClientService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiClientDtos.CreateResponse create(@Valid @RequestBody ApiClientDtos.CreateRequest request) {
        ApiClientService.ApiClientCreationResult result = apiClientService.create(TenantContext.requireTenantId(),
                request.name(), request.clientType(), request.scopes(), request.expiresAt());
        return ApiClientDtos.CreateResponse.from(result);
    }

    @GetMapping
    public List<ApiClientDtos.ApiClientResponse> list() {
        return apiClientService.list(TenantContext.requireTenantId()).stream()
                .map(ApiClientDtos.ApiClientResponse::from).toList();
    }

    @GetMapping("/{id}")
    public ApiClientDtos.ApiClientResponse get(@PathVariable UUID id) {
        return ApiClientDtos.ApiClientResponse.from(apiClientService.get(TenantContext.requireTenantId(), id));
    }

    @PostMapping("/{id}/revoke")
    public ApiClientDtos.ApiClientResponse revoke(@PathVariable UUID id) {
        return ApiClientDtos.ApiClientResponse.from(apiClientService.revoke(TenantContext.requireTenantId(), id));
    }
}
