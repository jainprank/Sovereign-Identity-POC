package com.idtrust.common.security;

import java.util.List;
import java.util.UUID;

/**
 * The result of successfully authenticating a machine-to-machine request (EPIC 14, FR-API-001
 * API Key / FR-API-002 OAuth Client) against a bearer credential that is <em>not</em> a Keycloak
 * JWT. {@code roles} are drawn from the same fixed role-code vocabulary
 * (PLATFORM_ADMIN/ORG_ADMIN/ISSUER/VERIFIER/HOLDER) that every controller's
 * {@code @PreAuthorize("hasAnyRole(...)")} annotation already hardcodes - see
 * {@code com.idtrust.apiplatform.client.ApiScopes} in the {@code apiplatform} module, the single
 * place those literals are declared for FR-API-003 (API Scope) validation.
 *
 * @param tenantId    the tenant this credential belongs to, bound directly to {@code TenantContext}
 * @param apiClientId identifies which {@code ApiClient} authenticated the request (audit/logging/rate-limiting)
 * @param roles       role codes granted to this credential, mapped 1:1 onto {@code ROLE_*} authorities
 */
public record ApiPrincipal(UUID tenantId, UUID apiClientId, List<String> roles) {
}
