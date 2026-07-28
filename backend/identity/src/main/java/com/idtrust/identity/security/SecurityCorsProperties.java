package com.idtrust.identity.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Binds {@code idtrust.security.cors.*} configuration (Baseline UI/UX section - the E15 frontend
 * scaffold under {@code frontend/} is served from a different origin than this API in local dev,
 * e.g. {@code http://localhost:5173} vs. {@code http://localhost:8080}).
 *
 * @param allowedOrigins exact origins (scheme+host+port, no path/wildcard) the browser-based
 *                        frontend is served from. Defaults to the Vite dev server's origin only -
 *                        every non-local environment must override this via
 *                        {@code CORS_ALLOWED_ORIGINS} with the real deployed frontend origin(s).
 */
@ConfigurationProperties(prefix = "idtrust.security.cors")
public record SecurityCorsProperties(List<String> allowedOrigins) {
}
