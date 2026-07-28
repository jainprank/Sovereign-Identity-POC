package com.idtrust.identity.user;

import com.idtrust.common.tenant.TenantContext;
import com.idtrust.common.web.PageResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Tenant-scoped user administration API. Requires the {@code X-Tenant-ID} header (resolved into
 * {@link TenantContext} by {@code TenantContextInterceptor}); user creation happens via
 * invitation acceptance, not directly through this API (see the {@code invitation} package).
 */
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN')")
    public PageResponse<UserDtos.Response> list(@RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "50") int size) {
        return PageResponse.of(userService.listByTenant(TenantContext.requireTenantId(), PageRequest.of(page, Math.min(size, 200))),
                UserDtos.Response::from);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN')")
    public UserDtos.Response get(@PathVariable UUID id) {
        return UserDtos.Response.from(userService.get(TenantContext.requireTenantId(), id));
    }

    @PostMapping("/{id}/suspend")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN')")
    public UserDtos.Response suspend(@PathVariable UUID id) {
        return UserDtos.Response.from(userService.suspend(TenantContext.requireTenantId(), id));
    }

    @PostMapping("/{id}/reactivate")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN')")
    public UserDtos.Response reactivate(@PathVariable UUID id) {
        return UserDtos.Response.from(userService.reactivate(TenantContext.requireTenantId(), id));
    }

    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN')")
    public UserDtos.Response deactivate(@PathVariable UUID id) {
        return UserDtos.Response.from(userService.deactivate(TenantContext.requireTenantId(), id));
    }
}
