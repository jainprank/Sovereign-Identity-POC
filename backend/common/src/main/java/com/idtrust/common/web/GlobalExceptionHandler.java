package com.idtrust.common.web;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/**
 * Translates exceptions into the standard {@link ApiError} shape for every controller in every
 * module (this advice is picked up platform-wide because it lives in the shared kernel and the
 * application component-scans {@code com.idtrust}).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> handleApiException(ApiException ex, HttpServletRequest request) {
        ApiError body = ApiError.of(ex.code(), ex.getMessage(), correlationId(request), ex.details());
        return ResponseEntity.status(ex.code().httpStatus()).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::toString)
                .toList();
        ApiError body = ApiError.of(ApiErrorCode.VALIDATION_ERROR, "Request validation failed", correlationId(request), details);
        return ResponseEntity.status(ApiErrorCode.VALIDATION_ERROR.httpStatus()).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception while processing {} {}", request.getMethod(), request.getRequestURI(), ex);
        ApiError body = ApiError.of(ApiErrorCode.INTERNAL_ERROR, "An unexpected error occurred", correlationId(request), List.of());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    private String correlationId(HttpServletRequest request) {
        String value = request.getHeader(CorrelationIdFilter.HEADER_NAME);
        return value == null || value.isBlank() ? "unknown" : value;
    }
}
