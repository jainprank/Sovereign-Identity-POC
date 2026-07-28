package com.idtrust.common.web;

import com.idtrust.common.tenant.CurrentApiClientContext;
import com.idtrust.common.tenant.CurrentUserContext;
import com.idtrust.common.tenant.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

/**
 * Resolves the {@code X-Tenant-ID} header (API contract standard, Architecture &amp;
 * Implementation Baseline section 9) into {@link TenantContext} for the lifetime of the request.
 *
 * <p>This interceptor does not itself require every request to carry the header: platform-level
 * endpoints (organization/tenant administration) operate across tenants and simply leave the
 * context unset, which downstream Postgres Row Level Security policies treat as "no tenant"
 * (fails closed - see the V3 migration). Endpoints that are inherently tenant-scoped are
 * expected to validate that a tenant was resolved themselves (see the identity module's
 * TenantMembershipFilter).
 *
 * <p>Also clears {@link CurrentUserContext} and {@link CurrentApiClientContext} at the end of
 * every request: those contexts are populated earlier in the request-processing pipeline by
 * {@code TenantMembershipFilter}/{@code ApiKeyAuthenticationFilter} (servlet filters, which run
 * before this MVC interceptor), but this interceptor is the last place still guaranteed to run for
 * every dispatched request, so it is the safest single place to guarantee all three thread-locals
 * are released back to the (pooled) request-handling thread.
 */
public class TenantContextInterceptor implements HandlerInterceptor {

    public static final String HEADER_NAME = "X-Tenant-ID";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String header = request.getHeader(HEADER_NAME);
        if (header != null && !header.isBlank()) {
            try {
                TenantContext.setTenantId(UUID.fromString(header.trim()));
            } catch (IllegalArgumentException e) {
                throw new ApiException(ApiErrorCode.INVALID_REQUEST, "X-Tenant-ID header is not a valid UUID");
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        TenantContext.clear();
        CurrentUserContext.clear();
        CurrentApiClientContext.clear();
    }
}
