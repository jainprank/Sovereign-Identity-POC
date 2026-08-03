package com.idtrust.common.tenant;

import java.util.Set;
import java.util.UUID;

/**
 * Holds the current request's resolved application user (as opposed to the raw JWT principal)
 * for the lifetime of the request thread. Populated once the identity module has confirmed the
 * authenticated principal is an active member of {@link TenantContext#getTenantId()}, and
 * consumed by services/audit logging to record the acting user.
 *
 * <p>Also carries the caller's Keycloak realm roles (the same {@code ROLE_*} authorities
 * {@code @PreAuthorize} checks against - see {@code KeycloakRealmRoleConverter}), so that
 * service-layer code which is not itself a {@code @RestController} - and so cannot use
 * {@code @PreAuthorize} - can still make role-conditional decisions (e.g. filtering a "list my
 * own resources" query to the caller's own rows for HOLDER, while leaving it unfiltered for
 * PLATFORM_ADMIN/ORG_ADMIN) without every module needing a direct dependency on Spring Security's
 * {@code SecurityContextHolder}.
 */
public final class CurrentUserContext {

    private static final ThreadLocal<UUID> CURRENT_USER = new ThreadLocal<>();
    private static final ThreadLocal<Set<String>> CURRENT_ROLES = new ThreadLocal<>();

    private CurrentUserContext() {
    }

    public static void setUserId(UUID userId) {
        CURRENT_USER.set(userId);
    }

    public static UUID getUserId() {
        return CURRENT_USER.get();
    }

    /** @param roles bare role names, e.g. {@code "HOLDER"} - without the {@code ROLE_} prefix. */
    public static void setRoles(Set<String> roles) {
        CURRENT_ROLES.set(roles);
    }

    public static boolean hasAnyRole(String... roles) {
        Set<String> current = CURRENT_ROLES.get();
        if (current == null) {
            return false;
        }
        for (String role : roles) {
            if (current.contains(role)) {
                return true;
            }
        }
        return false;
    }

    /** Must be called at the end of every request (e.g. in a {@code finally} block). */
    public static void clear() {
        CURRENT_USER.remove();
        CURRENT_ROLES.remove();
    }
}
