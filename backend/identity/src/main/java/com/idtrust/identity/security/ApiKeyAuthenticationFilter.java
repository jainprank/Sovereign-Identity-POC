package com.idtrust.identity.security;

import com.idtrust.common.security.ApiCredentialAuthenticator;
import com.idtrust.common.security.ApiPrincipal;
import com.idtrust.common.tenant.CurrentApiClientContext;
import com.idtrust.common.tenant.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Authenticates an API key (FR-API-001) or OAuth client-credentials access token (FR-API-002)
 * presented as a normal {@code Authorization: Bearer <token>} header - an alternate,
 * non-Keycloak path into the same {@code SecurityFilterChain} every human request goes through.
 *
 * <p><strong>Ordering:</strong> registered via
 * {@code .addFilterBefore(apiKeyAuthenticationFilter, BearerTokenAuthenticationFilter.class)} so
 * it always gets first look at the bearer token. If {@link ApiCredentialAuthenticator} does not
 * recognise the token (the common case: it is a genuine Keycloak JWT), this filter is a no-op and
 * the request falls through to the normal OAuth2 resource-server JWT filter unchanged.
 *
 * <p>The resulting {@link org.springframework.security.core.Authentication} is deliberately not a
 * {@code JwtAuthenticationToken}, so {@code TenantMembershipFilter}'s existing
 * {@code instanceof JwtAuthenticationToken} guard harmlessly no-ops for it - this filter is
 * therefore responsible for populating {@link TenantContext} (and {@link CurrentApiClientContext})
 * itself, mirroring what {@code TenantMembershipFilter} does for JWT-authenticated requests.
 * {@code CurrentUserContext} is deliberately left unset: there is no human user for machine
 * authentication, consistent with {@code AuditEvent.actorUserId()}'s documented "nullable for
 * system actions" precedent.
 *
 * <p>Granted authorities are {@code ROLE_<code>} for each of {@link ApiPrincipal#roles()} -
 * exactly {@code KeycloakRealmRoleConverter}'s convention - so every existing controller's
 * {@code @PreAuthorize("hasAnyRole(...)")} annotation works unchanged regardless of how the
 * request was authenticated.
 */
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final ApiCredentialAuthenticator authenticator;

    public ApiKeyAuthenticationFilter(ApiCredentialAuthenticator authenticator) {
        this.authenticator = authenticator;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(BEARER_PREFIX.length()).trim();
        Optional<ApiPrincipal> principal = authenticator.authenticate(token);
        if (principal.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        ApiPrincipal apiPrincipal = principal.get();
        List<GrantedAuthority> authorities = apiPrincipal.roles().stream()
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(apiPrincipal.apiClientId(), null, authorities));
        TenantContext.setTenantId(apiPrincipal.tenantId());
        CurrentApiClientContext.setApiClientId(apiPrincipal.apiClientId());
        filterChain.doFilter(request, response);
    }
}
