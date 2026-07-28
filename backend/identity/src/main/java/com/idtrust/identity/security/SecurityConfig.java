package com.idtrust.identity.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.idtrust.common.security.ApiCredentialAuthenticator;
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
    public SecurityFilterChain filterChain(HttpSecurity http, UserRepository userRepository, ObjectMapper objectMapper,
                                            ApiCredentialAuthenticator apiCredentialAuthenticator)
            throws Exception {
        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(new KeycloakRealmRoleConverter());

        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
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
                .addFilterAfter(new TenantMembershipFilter(userRepository), ExceptionTranslationFilter.class);

        return http.build();
    }
}
