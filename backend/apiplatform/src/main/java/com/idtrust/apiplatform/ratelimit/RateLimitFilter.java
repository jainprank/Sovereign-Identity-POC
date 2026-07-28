package com.idtrust.apiplatform.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.idtrust.apiplatform.config.ApiPlatformProperties;
import com.idtrust.common.tenant.CurrentApiClientContext;
import com.idtrust.common.tenant.TenantContext;
import com.idtrust.common.web.ApiError;
import com.idtrust.common.web.ApiErrorCode;
import com.idtrust.common.web.CorrelationIdFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * FR-API-004 (Rate Limit) - a simple in-memory fixed-window limiter over {@code /api/v1/**}.
 * Registered as a plain Spring Boot servlet filter (not part of the Spring Security filter chain)
 * at {@link Order} {@code 10}, i.e. strictly after Spring Security's {@code FilterChainProxy}
 * (registered at {@code SecurityProperties.DEFAULT_FILTER_ORDER == -100}) has already run its
 * internal authentication filters - so {@link TenantContext}/{@link CurrentApiClientContext} are
 * already populated by the time this filter keys its counter.
 *
 * <p>Keyed by, in order of preference: the authenticated API client id (FR-API-001/002 traffic),
 * else the resolved tenant id (human/JWT traffic), else the caller's remote address (unauthenticated
 * permitted endpoints, e.g. {@code /api/v1/dids/resolve}).
 *
 * <p><strong>Known limitation:</strong> in-memory and per-instance, not distributed - acceptable
 * for this single-instance modular monolith (no Redis client exists anywhere in this codebase);
 * revisit if horizontally scaled. Idle keys are never evicted; acceptable given the bounded number
 * of tenants/API clients expected, but would need an eviction policy at larger scale.
 */
@Component
@Order(10)
public class RateLimitFilter extends OncePerRequestFilter {

    private final ApiPlatformProperties properties;
    private final ObjectMapper objectMapper;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    public RateLimitFilter(ApiPlatformProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!request.getRequestURI().startsWith("/api/v1/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = resolveKey(request);
        long nowWindow = System.currentTimeMillis() / 60_000L;
        Window window = windows.compute(key, (k, existing) ->
                (existing == null || existing.windowId != nowWindow) ? new Window(nowWindow) : existing);

        int limit = properties.rateLimitRequestsPerMinute();
        if (window.count.incrementAndGet() > limit) {
            writeRateLimitedResponse(request, response);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private String resolveKey(HttpServletRequest request) {
        UUID apiClientId = CurrentApiClientContext.getApiClientId();
        if (apiClientId != null) {
            return "client:" + apiClientId;
        }
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            return "tenant:" + tenantId;
        }
        return "ip:" + request.getRemoteAddr();
    }

    private void writeRateLimitedResponse(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setStatus(ApiErrorCode.RATE_LIMITED.httpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        String correlationId = request.getHeader(CorrelationIdFilter.HEADER_NAME);
        ApiError body = ApiError.of(ApiErrorCode.RATE_LIMITED, "Too many requests - rate limit exceeded",
                correlationId == null || correlationId.isBlank() ? "unknown" : correlationId, List.of());
        objectMapper.writeValue(response.getWriter(), body);
    }

    private static final class Window {
        final long windowId;
        final AtomicInteger count = new AtomicInteger(0);

        Window(long windowId) {
            this.windowId = windowId;
        }
    }
}
