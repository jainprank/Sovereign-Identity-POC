package com.idtrust.common.web;

import java.util.List;

/**
 * Base type for exceptions that should be translated into the standard {@link ApiError}
 * response shape. Domain modules should throw (or subclass) this instead of raw
 * {@link RuntimeException}s so failures surface with a stable, catalogued error code.
 */
public class ApiException extends RuntimeException {

    private final ApiErrorCode code;
    private final List<String> details;

    public ApiException(ApiErrorCode code, String message) {
        this(code, message, List.of());
    }

    public ApiException(ApiErrorCode code, String message, List<String> details) {
        super(message);
        this.code = code;
        this.details = details;
    }

    public ApiErrorCode code() {
        return code;
    }

    public List<String> details() {
        return details;
    }
}
