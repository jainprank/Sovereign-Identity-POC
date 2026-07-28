package com.idtrust.apiplatform.client;

import com.idtrust.common.web.ApiErrorCode;
import com.idtrust.common.web.ApiException;

import java.util.List;
import java.util.Set;

/**
 * FR-API-003 (API Scope) - an {@link ApiClient}'s scopes are validated against this fixed set of
 * role codes rather than a separate, newly-invented scope vocabulary (e.g. {@code credentials:read}).
 * This is the exact same PLATFORM_ADMIN/ORG_ADMIN/ISSUER/VERIFIER/HOLDER literal set every
 * controller across the codebase already hardcodes in its {@code @PreAuthorize("hasAnyRole(...)")}
 * annotations (e.g. {@code TrustRegistryController}) - deliberately hardcoded here too, rather than
 * querying {@code identity.rbac.Role} at runtime, so this module does not need a new Gradle
 * dependency on {@code identity} for something every other module already treats as a fixed,
 * well-known literal set.
 */
public final class ApiScopes {

    public static final Set<String> VALID_ROLE_CODES = Set.of(
            "PLATFORM_ADMIN", "ORG_ADMIN", "ISSUER", "VERIFIER", "HOLDER");

    private ApiScopes() {
    }

    /** @throws ApiException INVALID_REQUEST if any requested scope is not one of {@link #VALID_ROLE_CODES}. */
    public static void validate(List<String> scopes) {
        if (scopes == null || scopes.isEmpty()) {
            throw new ApiException(ApiErrorCode.INVALID_REQUEST, "At least one scope is required");
        }
        for (String scope : scopes) {
            if (!VALID_ROLE_CODES.contains(scope)) {
                throw new ApiException(ApiErrorCode.INVALID_REQUEST,
                        "Unknown scope: " + scope + " - must be one of " + VALID_ROLE_CODES);
            }
        }
    }
}
