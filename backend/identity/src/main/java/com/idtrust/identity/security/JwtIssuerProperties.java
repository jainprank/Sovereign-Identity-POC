package com.idtrust.identity.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Binds {@code idtrust.security.jwt.*} configuration, consumed by {@link SecurityConfig}'s custom
 * {@code JwtDecoder} bean.
 *
 * <p>Local dev/testing complication this exists to solve: Keycloak stamps a JWT's {@code iss}
 * claim from whatever Host header the authorization request arrived on, so the same realm issues
 * tokens with a different {@code iss} depending on which address the client used to reach it - a
 * browser on this machine uses {@code localhost}, the Android emulator uses its {@code 10.0.2.2}
 * host alias, and a real device on the LAN uses this machine's LAN IP. Spring's default
 * {@code issuer-uri}-based auto-configuration accepts exactly one issuer value and 401s everything
 * else, so a custom decoder validates against a list instead - see {@code acceptedIssuers}.
 *
 * @param jwksUri          fixed JWK Set endpoint the backend itself fetches signing keys from -
 *                         always reachable as {@code localhost} since the backend and Keycloak run
 *                         on the same host, regardless of what hostname a given client used.
 * @param acceptedIssuers  every {@code iss} claim value this backend will accept a token from.
 *                         Must include the exact realm issuer URL for each hostname a client might
 *                         reach Keycloak through. Comma-separated via {@code JWT_ACCEPTED_ISSUERS}.
 */
@ConfigurationProperties(prefix = "idtrust.security.jwt")
public record JwtIssuerProperties(String jwksUri, List<String> acceptedIssuers) {
}
