package com.idtrust.apiplatform.log;

import com.idtrust.common.web.CorrelationIdFilter;
import com.idtrust.common.web.TenantContextInterceptor;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * FR-API-007 (API Logs) - persists one {@link ApiRequestLog} row per completed
 * {@code /api/v1/**} request. Registered at {@code @Order(0)}, strictly after Spring Security's
 * {@code FilterChainProxy} (order {@code -100}) so the request is already authenticated/tenant-
 * resolved by the time it reaches this filter, but <em>outside</em> (a lower order than)
 * {@code ratelimit.RateLimitFilter} (order {@code 10}) so a request rejected with 429 is still
 * captured with its real outcome and total duration.
 *
 * <p>Reads the resolved tenant/API-client id from the request attributes
 * {@link TenantContextInterceptor} stashes there, rather than from
 * {@code TenantContext}/{@code CurrentApiClientContext} directly: this filter's {@code finally}
 * block only runs once {@code filterChain.doFilter} returns, which is <em>after</em>
 * {@code DispatcherServlet} has already run {@link TenantContextInterceptor#afterCompletion}
 * and cleared those thread-locals - reading them directly here always observed {@code null}.
 *
 * <p>Also the single choke point for FR-ANL-005 (API Metrics): every request already measured
 * here for {@link ApiRequestLog} additionally records an {@code idtrust.api.requests} Micrometer
 * timer, tagged by {@code method}/{@code status} - deliberately not by raw path (an unbounded,
 * id-containing cardinality Prometheus would never forgive).
 */
@Component
@Order(0)
public class ApiRequestLoggingFilter extends OncePerRequestFilter {

    private final ApiRequestLogRepository apiRequestLogRepository;
    private final MeterRegistry meterRegistry;

    public ApiRequestLoggingFilter(ApiRequestLogRepository apiRequestLogRepository, MeterRegistry meterRegistry) {
        this.apiRequestLogRepository = apiRequestLogRepository;
        this.meterRegistry = meterRegistry;
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
                    (UUID) request.getAttribute(TenantContextInterceptor.RESOLVED_TENANT_ID_ATTRIBUTE),
                    (UUID) request.getAttribute(TenantContextInterceptor.RESOLVED_API_CLIENT_ID_ATTRIBUTE),
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    durationMs,
                    correlationId));
            meterRegistry.timer("idtrust.api.requests", "method", request.getMethod(),
                            "status", String.valueOf(response.getStatus()))
                    .record(durationMs, TimeUnit.MILLISECONDS);
        }
    }
}
