package com.idtrust.identity.security;

import com.idtrust.common.tenant.CurrentUserContext;
import com.idtrust.common.tenant.TenantContext;
import com.idtrust.identity.user.AppUser;
import com.idtrust.identity.user.UserRepository;
import com.idtrust.identity.user.UserStatus;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

/**
 * Bridges the authenticated Keycloak JWT to a local, tenant-scoped {@link AppUser} record: bearer
 * tokens prove *who* the caller is platform-wide, but every non-platform-administration endpoint
 * additionally requires that the caller is an active member of the tenant named by the
 * {@code X-Tenant-ID} header (three-layer tenant isolation - Architecture &amp; Implementation
 * Baseline, section 23).
 *
 * <p><strong>Ordering:</strong> registered via
 * {@code .addFilterAfter(tenantMembershipFilter, ExceptionTranslationFilter.class)} so that:
 * <ol>
 *   <li>it runs after {@code BearerTokenAuthenticationFilter}, i.e. after JWT authentication has
 *       already populated the {@link SecurityContextHolder}; and</li>
 *   <li>the {@link AccessDeniedException} it throws is still caught by
 *       {@code ExceptionTranslationFilter} (which wraps everything positioned <em>after</em> it in
 *       the chain) and converted to a 403 via {@link RestAccessDeniedHandler}, rather than
 *       bubbling up as an unhandled server error.</li>
 * </ol>
 *
 * <p><strong>Also note:</strong> this filter - not {@code TenantContextInterceptor} - is the
 * authoritative place {@link TenantContext} is populated for authenticated requests, because
 * servlet filters run before Spring MVC's {@code HandlerInterceptor}s in the request-processing
 * pipeline; by the time {@code TenantContextInterceptor.preHandle} runs, membership would already
 * need to have been checked. {@code TenantContextInterceptor} still independently re-parses the
 * header (harmless/idempotent) so that platform-administration endpoints, which have no JWT
 * subject-to-user linkage to check, keep working unchanged.
 *
 * <p><strong>Known limitation:</strong> resolves the user first by {@code keycloak_subject_id},
 * falling back to the JWT {@code email} claim. Real Keycloak realm/client provisioning and
 * first-login subject-id linkage beyond invitation acceptance is out of scope for this pass.
 */
public class TenantMembershipFilter extends OncePerRequestFilter {

    public static final String TENANT_HEADER = "X-Tenant-ID";

    private final UserRepository userRepository;

    public TenantMembershipFilter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)) {
            filterChain.doFilter(request, response);
            return;
        }

        String header = request.getHeader(TENANT_HEADER);
        if (header == null || header.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        UUID tenantId;
        try {
            tenantId = UUID.fromString(header.trim());
        } catch (IllegalArgumentException ex) {
            // Malformed header: leave rejection to TenantContextInterceptor downstream, which
            // reports it via the standard ApiError shape (Filters run outside MVC's exception
            // handling scope, so this filter must not throw ApiException itself).
            filterChain.doFilter(request, response);
            return;
        }

        AppUser user = resolveUser(tenantId, jwtAuthentication.getToken());
        if (user == null || user.getStatus() != UserStatus.ACTIVE) {
            throw new AccessDeniedException("Not an active member of tenant " + tenantId);
        }

        TenantContext.setTenantId(tenantId);
        CurrentUserContext.setUserId(user.getId());
        filterChain.doFilter(request, response);
    }

    private AppUser resolveUser(UUID tenantId, Jwt jwt) {
        Optional<AppUser> bySubject = userRepository.findByTenantIdAndKeycloakSubjectId(tenantId, jwt.getSubject());
        if (bySubject.isPresent()) {
            return bySubject.get();
        }
        String email = jwt.getClaimAsString("email");
        if (email == null) {
            return null;
        }
        return userRepository.findByTenantIdAndEmail(tenantId, email).orElse(null);
    }
}
