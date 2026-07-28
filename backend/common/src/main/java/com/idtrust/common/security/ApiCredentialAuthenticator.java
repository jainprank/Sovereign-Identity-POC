package com.idtrust.common.security;

import java.util.Optional;

/**
 * Verifies a bearer credential that is not a Keycloak-issued JWT - an API key (FR-API-001) or an
 * OAuth client-credentials access token (FR-API-002) - and resolves it to an {@link ApiPrincipal}.
 * Mirrors {@code AuditEventPublisher}/{@code HederaAnchorPublisher}'s decoupling: the interface
 * lives here so {@code identity.security.SecurityConfig} can wire the filter chain without the
 * {@code identity} module depending on the {@code apiplatform} module, which provides the single
 * real implementation ({@code apiplatform.auth.ApiClientCredentialAuthenticator}).
 *
 * <p>Never throws - an unrecognised or expired credential simply resolves to {@link Optional#empty()},
 * letting the request fall through to the normal Keycloak JWT resource-server path (which will
 * itself reject it as unauthenticated) rather than short-circuiting with an error of its own.
 */
public interface ApiCredentialAuthenticator {

    Optional<ApiPrincipal> authenticate(String bearerToken);
}
