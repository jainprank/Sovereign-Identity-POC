package com.idtrust.common.web;

import java.time.Instant;
import java.util.List;

/**
 * Standard error response body used across every IDTrust API, per the Architecture &amp;
 * Implementation Baseline, section 10 ("API Error Catalogue").
 */
public record ApiError(
        Instant timestamp,
        int status,
        String code,
        String message,
        String correlationId,
        List<String> details
) {

    public static ApiError of(ApiErrorCode code, String message, String correlationId, List<String> details) {
        return new ApiError(Instant.now(), code.httpStatus().value(), code.name(), message, correlationId, details);
    }
}
