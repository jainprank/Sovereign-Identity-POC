package com.idtrust.common.tenant;

import java.util.UUID;

/**
 * Holds the current request's tenant identity for the lifetime of the request thread.
 *
 * <p>Every tenant-owned table/entity carries a {@code tenant_id} column, and access must be
 * enforced at the API layer, the service layer, and the database layer (Postgres RLS) - see
 * the IDTrust Architecture &amp; Implementation Baseline, section 23 ("Tenant Isolation").
 *
 * <p>This class only satisfies the API/service layer concern. It is populated by a servlet
 * filter/interceptor once authentication (E02) is implemented, and consumed by repositories to
 * scope queries and by the database layer to set the RLS session variable (E03).
 */
public final class TenantContext {

    private static final ThreadLocal<UUID> CURRENT_TENANT = new ThreadLocal<>();
    private static final ThreadLocal<UUID> CURRENT_ORGANIZATION = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void setTenantId(UUID tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    public static UUID getTenantId() {
        return CURRENT_TENANT.get();
    }

    public static void setOrganizationId(UUID organizationId) {
        CURRENT_ORGANIZATION.set(organizationId);
    }

    public static UUID getOrganizationId() {
        return CURRENT_ORGANIZATION.get();
    }

    public static UUID requireTenantId() {
        UUID tenantId = CURRENT_TENANT.get();
        if (tenantId == null) {
            throw new IllegalStateException("No tenant context is bound to the current thread");
        }
        return tenantId;
    }

    /** Must be called at the end of every request (e.g. in a {@code finally} block) to avoid leaking context between pooled threads. */
    public static void clear() {
        CURRENT_TENANT.remove();
        CURRENT_ORGANIZATION.remove();
    }
}
