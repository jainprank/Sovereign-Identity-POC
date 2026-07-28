package com.idtrust.apiplatform.log;

import com.idtrust.common.tenant.CurrentApiClientContext;
import com.idtrust.common.tenant.TenantContext;
import com.idtrust.common.web.CorrelationIdFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * FR-API-007 (API Logs) - persists one {@link ApiRequestLog} row per completed
 * {@code /api/v1/**} request. Registered at {@code @Order(0)}, strictly after Spring Security's
 * {@code FilterChainProxy} (order {@code -100}) so {@link TenantContext}/
 * {@link CurrentApiClientContext} are already populated, but <em>outside</em> (a lower order than)
 * {@code ratelimit.RateLimitFilter} (order {@code 10}) so a request rejected with 429 is still
 * captured with its real outcome and total duration.
 */
@Component
@Order(0)
public class ApiRequestLoggingFilter extends OncePerRequestFilter {

    private final ApiRequestLogRepository apiRequestLogRepository;

    public ApiRequestLoggingFilter(ApiRequestLogRepository apiRequestLogRepository) {
        this.apiRequestLogRepository = apiRequestLogRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!request.getRequestURI().startsWith("/api/v1/")) {
            filterChain.doFilter(request, response);
            return;
        }

        long startedAt = System.currentTimeMillis();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = System.currentTimeMillis() - startedAt;
            String correlationId = response.getHeader(CorrelationIdFilter.HEADER_NAME);
            apiRequestLogRepository.save(new ApiRequestLog(
                    TenantContext.getTenantId(),
                    CurrentApiClientContext.getApiClientId(),
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    durationMs,
                    correlationId));
        }
    }
}
