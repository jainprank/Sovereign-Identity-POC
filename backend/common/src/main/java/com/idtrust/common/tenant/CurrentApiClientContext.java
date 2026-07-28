package com.idtrust.common.tenant;

import java.util.UUID;

/**
 * Holds the current request's authenticated {@code ApiClient} id (EPIC 14, FR-API-001/002) for
 * the lifetime of the request thread - {@code null} for every human, Keycloak-JWT-authenticated
 * request. Populated by {@code identity.security.ApiKeyAuthenticationFilter} alongside
 * {@link TenantContext}, and consumed by the {@code apiplatform} module's rate-limiting
 * (FR-API-004) and API request logging (FR-API-007) filters to key/attribute machine traffic
 * without those filters needing to inspect the concrete {@code Authentication} type.
 */
public final class CurrentApiClientContext {

    private static final ThreadLocal<UUID> CURRENT_API_CLIENT = new ThreadLocal<>();

    private CurrentApiClientContext() {
    }

    public static void setApiClientId(UUID apiClientId) {
        CURRENT_API_CLIENT.set(apiClientId);
    }

    public static UUID getApiClientId() {
        return CURRENT_API_CLIENT.get();
    }

    /** Must be called at the end of every request (e.g. in a {@code finally} block). */
    public static void clear() {
        CURRENT_API_CLIENT.remove();
    }
}
