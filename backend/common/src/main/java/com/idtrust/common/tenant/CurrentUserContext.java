package com.idtrust.common.tenant;

import java.util.UUID;

/**
 * Holds the current request's resolved application user (as opposed to the raw JWT principal)
 * for the lifetime of the request thread. Populated once the identity module has confirmed the
 * authenticated principal is an active member of {@link TenantContext#getTenantId()}, and
 * consumed by services/audit logging to record the acting user.
 */
public final class CurrentUserContext {

    private static final ThreadLocal<UUID> CURRENT_USER = new ThreadLocal<>();

    private CurrentUserContext() {
    }

    public static void setUserId(UUID userId) {
        CURRENT_USER.set(userId);
    }

    public static UUID getUserId() {
        return CURRENT_USER.get();
    }

    /** Must be called at the end of every request (e.g. in a {@code finally} block). */
    public static void clear() {
        CURRENT_USER.remove();
    }
}
