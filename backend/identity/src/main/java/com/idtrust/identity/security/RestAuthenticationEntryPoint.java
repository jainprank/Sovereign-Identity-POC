package com.idtrust.identity.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.idtrust.common.web.ApiError;
import com.idtrust.common.web.ApiErrorCode;
import com.idtrust.common.web.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;
import java.util.List;

/** Converts unauthenticated-access failures into the standard {@link ApiError} JSON shape. */
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        response.setStatus(ApiErrorCode.UNAUTHORIZED.httpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        String correlationId = request.getHeader(CorrelationIdFilter.HEADER_NAME);
        ApiError body = ApiError.of(ApiErrorCode.UNAUTHORIZED, "Authentication is required to access this resource",
                correlationId == null || correlationId.isBlank() ? "unknown" : correlationId, List.of());
        objectMapper.writeValue(response.getWriter(), body);
    }
}
