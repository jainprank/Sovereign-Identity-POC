package com.idtrust.identity.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.idtrust.common.web.ApiError;
import com.idtrust.common.web.ApiErrorCode;
import com.idtrust.common.web.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;
import java.util.List;

/** Converts authorization failures (role checks, tenant-membership checks) into {@link ApiError} JSON. */
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public RestAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)
            throws IOException {
        response.setStatus(ApiErrorCode.FORBIDDEN.httpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        String correlationId = request.getHeader(CorrelationIdFilter.HEADER_NAME);
        ApiError body = ApiError.of(ApiErrorCode.FORBIDDEN, accessDeniedException.getMessage(),
                correlationId == null || correlationId.isBlank() ? "unknown" : correlationId, List.of());
        objectMapper.writeValue(response.getWriter(), body);
    }
}
