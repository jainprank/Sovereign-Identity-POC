/**
 * API &amp; Developer Platform module - implemented in E14, corresponding to EPIC 14 in the
 * Architecture &amp; Implementation Baseline (FR-API-001 through 012).
 *
 * <p>Provides the platform's machine-to-machine surface: API keys and OAuth 2.0 client-credentials
 * clients (FR-API-001/002) as an alternate, non-Keycloak authentication path into the same
 * {@code SecurityFilterChain} every human request goes through (see
 * {@code identity.security.ApiKeyAuthenticationFilter}); scopes validated against the platform's
 * existing fixed role-code vocabulary (FR-API-003, see {@link com.idtrust.apiplatform.client.ApiScopes});
 * an in-memory request-rate limiter (FR-API-004); generated OpenAPI/Swagger UI documentation
 * (FR-API-006); a persisted, queryable API request log (FR-API-007); and outbound webhooks with
 * HMAC-signed delivery and manual retry (FR-API-008/009).
 *
 * <p>FR-API-005 (API Versioning) requires no code here - the platform's universal {@code /api/v1/}
 * controller-path prefix, already in place since E01, satisfies it. FR-API-010 (Sandbox),
 * FR-API-011 (Developer Portal) and FR-API-012 (SDK) are out of scope for this pass - see the root
 * README's E14 section for the documented rationale.
 */
package com.idtrust.apiplatform;
