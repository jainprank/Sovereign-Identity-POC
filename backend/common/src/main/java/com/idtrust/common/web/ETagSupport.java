package com.idtrust.common.web;

/**
 * Backs the {@code ETag}/{@code If-Match} optimistic-concurrency contract (Hardened API
 * Specification): controllers stamp a JPA {@code @Version}-backed counter onto their GET/PATCH
 * responses via {@link #toHeaderValue(long)}, and validate a client-supplied {@code If-Match}
 * header against the current counter via {@link #requireMatch(String, long)} before applying a
 * PATCH, so a client updating a stale copy of a resource gets a 412 instead of silently
 * overwriting a concurrent change.
 */
public final class ETagSupport {

    private ETagSupport() {
    }

    public static String toHeaderValue(long version) {
        return Long.toString(version);
    }

    /**
     * No-ops if {@code ifMatchHeader} is absent or {@code *} (client didn't read the resource
     * first, or explicitly opted out of the check). Otherwise throws
     * {@link ApiErrorCode#PRECONDITION_FAILED} if it doesn't match {@code currentVersion}.
     */
    public static void requireMatch(String ifMatchHeader, long currentVersion) {
        if (ifMatchHeader == null || ifMatchHeader.isBlank()) {
            return;
        }
        String trimmed = ifMatchHeader.trim();
        if (trimmed.equals("*")) {
            return;
        }
        String unquoted = trimmed.replaceFirst("^W/", "").replaceAll("^\"|\"$", "");
        long expected;
        try {
            expected = Long.parseLong(unquoted);
        } catch (NumberFormatException e) {
            throw new ApiException(ApiErrorCode.PRECONDITION_FAILED, "Malformed If-Match header: " + ifMatchHeader);
        }
        if (expected != currentVersion) {
            throw new ApiException(ApiErrorCode.PRECONDITION_FAILED,
                    "Resource has changed since the If-Match version was read (expected " + expected
                            + ", current " + currentVersion + ")");
        }
    }
}
