package com.idtrust.identity.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.idtrust.common.tenant.TenantContext;
import com.idtrust.common.web.ApiError;
import com.idtrust.common.web.ApiErrorCode;
import com.idtrust.common.web.CorrelationIdFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Implements the {@code Idempotency-Key} header contract (Hardened API Specification): a client
 * may replay the exact same POST/PUT/PATCH request - same tenant, endpoint, key and body - any
 * number of times and get back the original response instead of re-executing the operation, while
 * a replay with the same key but a different body is rejected as a conflict.
 *
 * <p><strong>Ordering:</strong> registered via
 * {@code .addFilterAfter(idempotencyFilter, TenantMembershipFilter.class)} so that
 * {@link TenantContext#getTenantId()} is already populated for JWT-authenticated requests by the
 * time this filter runs.
 *
 * <p><strong>Known limitation:</strong> {@code idempotency_keys} rows are tenant-scoped
 * ({@code tenant_id NOT NULL}), so requests made with no resolved tenant context (e.g.
 * platform-administration endpoints such as organization/tenant creation) are not covered by this
 * filter. Extending idempotency to platform-level operations is a separate, not-yet-scoped change.
 */
public class IdempotencyFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "Idempotency-Key";

    private static final Set<String> IDEMPOTENT_METHODS = Set.of(
            HttpMethod.POST.name(), HttpMethod.PUT.name(), HttpMethod.PATCH.name());

    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final ObjectMapper objectMapper;

    public IdempotencyFilter(IdempotencyKeyRepository idempotencyKeyRepository, ObjectMapper objectMapper) {
        this.idempotencyKeyRepository = idempotencyKeyRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String idempotencyKey = request.getHeader(HEADER_NAME);
        UUID tenantId = TenantContext.getTenantId();

        if (idempotencyKey == null || idempotencyKey.isBlank()
                || tenantId == null
                || !IDEMPOTENT_METHODS.contains(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String endpoint = request.getMethod() + " " + request.getRequestURI();
        CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(request);
        String requestHash = sha256Hex(cachedRequest.getCachedBody());

        Optional<IdempotencyKey> existing = idempotencyKeyRepository
                .findByTenantIdAndIdempotencyKeyAndEndpoint(tenantId, idempotencyKey, endpoint);

        if (existing.isPresent()) {
            IdempotencyKey record = existing.get();
            if (!record.getRequestHash().equals(requestHash)) {
                writeConflict(request, response);
                return;
            }
            replay(response, record);
            return;
        }

        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
        filterChain.doFilter(cachedRequest, responseWrapper);

        if (responseWrapper.getStatus() < 500) {
            byte[] body = responseWrapper.getContentAsByteArray();
            String responseBody = body.length == 0 ? null : new String(body, StandardCharsets.UTF_8);
            saveIfAbsent(tenantId, idempotencyKey, endpoint, requestHash, responseWrapper.getStatus(), responseBody);
        }
        responseWrapper.copyBodyToResponse();
    }

    private void saveIfAbsent(UUID tenantId, String idempotencyKey, String endpoint, String requestHash,
                               int status, String responseBody) {
        // Guards against a race between the read above and this write (two concurrent identical
        // retries); the unique (tenant_id, idempotency_key, endpoint) constraint is the real
        // safety net, this check just avoids surfacing a benign constraint-violation as an error.
        if (idempotencyKeyRepository.findByTenantIdAndIdempotencyKeyAndEndpoint(tenantId, idempotencyKey, endpoint).isPresent()) {
            return;
        }
        idempotencyKeyRepository.save(new IdempotencyKey(tenantId, idempotencyKey, endpoint, requestHash, status,
                responseBody, Instant.now().plus(24, ChronoUnit.HOURS)));
    }

    private void replay(HttpServletResponse response, IdempotencyKey record) throws IOException {
        response.setStatus(record.getResponseStatus());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Idempotent-Replayed", "true");
        if (record.getResponseBody() != null) {
            response.getWriter().write(record.getResponseBody());
        }
    }

    private void writeConflict(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String correlationId = MDC.get(CorrelationIdFilter.MDC_KEY);
        ApiError error = ApiError.of(ApiErrorCode.RESOURCE_CONFLICT,
                "Idempotency-Key '" + request.getHeader(HEADER_NAME) + "' was already used with a different request body",
                correlationId, List.of());
        response.setStatus(ApiErrorCode.RESOURCE_CONFLICT.httpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), error);
    }

    private static String sha256Hex(byte[] body) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(body));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
