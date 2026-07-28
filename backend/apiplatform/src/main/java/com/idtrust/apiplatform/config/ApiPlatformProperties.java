package com.idtrust.apiplatform.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds {@code idtrust.apiplatform.*} configuration.
 *
 * @param rateLimitRequestsPerMinute FR-API-004 (Rate Limit) - the fixed-window request budget per
 *                                    caller (API client id if authenticated via
 *                                    {@code ApiKeyAuthenticationFilter}, otherwise tenant id, else
 *                                    remote address). Enforced in-memory by
 *                                    {@code ratelimit.RateLimitFilter} - not distributed; revisit if
 *                                    this modular monolith is ever horizontally scaled (no Redis
 *                                    client exists anywhere in this codebase yet).
 * @param oauthAccessTokenTtlSeconds FR-API-002 (OAuth Client) - how long an access token minted by
 *                                    {@code POST /api/v1/oauth/token} remains valid.
 */
@ConfigurationProperties(prefix = "idtrust.apiplatform")
public record ApiPlatformProperties(int rateLimitRequestsPerMinute, long oauthAccessTokenTtlSeconds) {
}
