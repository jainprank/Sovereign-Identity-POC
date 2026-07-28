package com.idtrust.identity.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.idtrust.common.security.ApiCredentialAuthenticator;
import com.idtrust.identity.idempotency.IdempotencyFilter;
import com.idtrust.identity.idempotency.IdempotencyKeyRepository;
import com.idtrust.identity.user.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.ExceptionTranslationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Wires the identity module's API into Spring Security's OAuth2 Resource Server support
 * (Keycloak-issued JWTs), per the Architecture &amp; Implementation Baseline, section 11
 * ("Authentication &amp; Authorization"). Stateless: every request must carry a bearer token, no
 * server-side session state is kept.
 *
 * <p>{@link ApiKeyAuthenticationFilter}, registered ahead of {@link BearerTokenAuthenticationFilter},
 * adds a second, alternate bearer-credential path (EPIC 14, FR-API-001 API Key / FR-API-002 OAuth
 * Client) into this same filter chain - see its Javadoc for how it interoperates with
 * {@link TenantMembershipFilter} and every controller's existing {@code @PreAuthorize} checks.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource(SecurityCorsProperties corsProperties) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(corsProperties.allowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        // X-Tenant-ID (three-layer tenant isolation) and Authorization (bearer token) are the two
        // headers the frontend scaffold under frontend/ needs to send on every authenticated call.
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Tenant-ID"));
        configuration.setExposedHeaders(List.of("Content-Disposition"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, UserRepository userRepository, ObjectMapper objectMapper,
                                            ApiCredentialAuthenticator apiCredentialAuthenticator,
                                            CorsConfigurationSource corsConfigurationSource,
                                            IdempotencyKeyRepository idempotencyKeyRepository)
            throws Exception {
        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(new KeycloakRealmRoleConverter());

        http
                // EPIC 21 threat model / Master Spec section 33 (Security Architecture) - CSRF
                // protection is a cookie-session concern; this API is stateless bearer-token-only
                // (no cookie is ever set for authentication), so there is no ambient credential a
                // cross-site request could ride on - disabling is a documented architectural fit,
                // not a skipped control. See the root README's E15 section.
                .csrf(csrf -> csrf.disable())
                // The E15 frontend scaffold (frontend/) is served from its own origin and calls
                // this API directly with a Keycloak-issued bearer token - see
                // SecurityCorsProperties' Javadoc for the allow-list this reads from.
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // EPIC 21 threat model (XSS/clickjacking) / Master Spec section 33 - security
                // response headers applied to every response. Spring Security's own defaults
                // already cover X-Content-Type-Options and a conservative HSTS; CSP/frame-options/
                // referrer-policy/permissions-policy are added explicitly since this is a JSON-only
                // API serving no HTML the browser needs to render trustingly.
                .headers(headers -> headers
                        .frameOptions(frame -> frame.deny())
                        .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'none'; frame-ancestors 'none'"))
                        .referrerPolicy(referrer -> referrer
                                .policy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                        .permissionsPolicy(permissions -> permissions
                                .policy("geolocation=(), camera=(), microphone=()")))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/v1/health", "/actuator/**").permitAll()
                        .requestMatchers("/api/v1/invitations/accept").permitAll()
                        .requestMatchers("/api/v1/dids/resolve").permitAll()
                        .requestMatchers("/api/v1/oid4vci/issuer-metadata", "/api/v1/oid4vci/token",
                                "/api/v1/oid4vci/credential").permitAll()
                        .requestMatchers("/api/v1/verification-requests/*/present").permitAll()
                        // FR-API-002 (OAuth Client) - the token endpoint itself authenticates the
                        // client via its own client_id/client_secret request body, not a bearer token.
                        .requestMatchers("/api/v1/oauth/token").permitAll()
                        // FR-API-006 (OpenAPI) - generated API docs/Swagger UI are not tenant data.
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(new RestAuthenticationEntryPoint(objectMapper))
                        .accessDeniedHandler(new RestAccessDeniedHandler(objectMapper)))
                .addFilterBefore(new ApiKeyAuthenticationFilter(apiCredentialAuthenticator), BearerTokenAuthenticationFilter.class)
                .addFilterAfter(new TenantMembershipFilter(userRepository), ExceptionTranslationFilter.class)
                .addFilterAfter(new IdempotencyFilter(idempotencyKeyRepository, objectMapper), TenantMembershipFilter.class);

        return http.build();
    }
}
