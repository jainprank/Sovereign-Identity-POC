package com.idtrust.common.web;

import org.springframework.http.HttpStatus;

/**
 * The platform-wide error code catalogue, as defined in the IDTrust Architecture &amp;
 * Implementation Baseline, section 10 ("API Error Catalogue"). Every module should reuse these
 * codes rather than inventing new ones, so API consumers get a single, stable error vocabulary.
 */
public enum ApiErrorCode {

    INVALID_REQUEST(HttpStatus.BAD_REQUEST),
    INVALID_DID(HttpStatus.BAD_REQUEST),
    INVALID_CREDENTIAL(HttpStatus.BAD_REQUEST),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED),
    FORBIDDEN(HttpStatus.FORBIDDEN),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND),
    RESOURCE_CONFLICT(HttpStatus.CONFLICT),
    VALIDATION_ERROR(HttpStatus.UNPROCESSABLE_ENTITY),
    RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR),
    EXTERNAL_SERVICE_ERROR(HttpStatus.BAD_GATEWAY),
    SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE);

    private final HttpStatus httpStatus;

    ApiErrorCode(HttpStatus httpStatus) {
        this.httpStatus = httpStatus;
    }

    public HttpStatus httpStatus() {
        return httpStatus;
    }
}
