package com.idtrust.apiplatform.auth;

import com.idtrust.apiplatform.client.ApiClient;
import com.idtrust.apiplatform.client.ApiClientRepository;
import com.idtrust.apiplatform.client.ApiClientType;
import com.idtrust.apiplatform.config.ApiPlatformProperties;
import com.idtrust.common.audit.AuditEvent;
import com.idtrust.common.audit.AuditEventPublisher;
import com.idtrust.common.web.ApiErrorCode;
import com.idtrust.common.web.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

/**
 * Implements FR-API-002 (OAuth Client)'s {@code client_credentials} grant: mints a genuine,
 * server-verified opaque access token for an already-provisioned {@code OAUTH_CLIENT}-type
 * {@link ApiClient} (see {@code client.ApiClientService#create} for provisioning). Never attempts
 * real Keycloak Admin REST API client registration - this is a small, self-contained
 * authorization-server surface, not a Keycloak integration.
 */
@Service
public class OAuthTokenService {

    private final ApiClientRepository apiClientRepository;
    private final ApiAccessTokenRepository accessTokenRepository;
    private final ApiPlatformProperties properties;
    private final AuditEventPublisher auditEventPublisher;

    public OAuthTokenService(ApiClientRepository apiClientRepository, ApiAccessTokenRepository accessTokenRepository,
                              ApiPlatformProperties properties, AuditEventPublisher auditEventPublisher) {
        this.apiClientRepository = apiClientRepository;
        this.accessTokenRepository = accessTokenRepository;
        this.properties = properties;
        this.auditEventPublisher = auditEventPublisher;
    }

    @Transactional
    public MintedToken mint(String clientId, String clientSecret) {
        ApiClient client = apiClientRepository.findByClientIdAndSecretHash(clientId, CredentialHasher.hash(clientSecret))
                .orElseThrow(() -> new ApiException(ApiErrorCode.UNAUTHORIZED, "Invalid client_id or client_secret"));
        if (client.getClientType() != ApiClientType.OAUTH_CLIENT) {
            throw new ApiException(ApiErrorCode.UNAUTHORIZED, "Invalid client_id or client_secret");
        }
        if (!client.isUsable()) {
            throw new ApiException(ApiErrorCode.UNAUTHORIZED, "This OAuth client is revoked or expired");
        }

        String tokenValue = CredentialHasher.generateSecret();
        long ttlSeconds = properties.oauthAccessTokenTtlSeconds();
        Instant expiresAt = Instant.now().plusSeconds(ttlSeconds);
        ApiAccessToken token = new ApiAccessToken(client.getTenantId(), client.getId(), CredentialHasher.hash(tokenValue), expiresAt);
        accessTokenRepository.save(token);

        client.recordUsage();
        apiClientRepository.save(client);

        auditEventPublisher.publish(AuditEvent.of(client.getTenantId(), null, "OAUTH_TOKEN_ISSUED", "API_CLIENT",
                client.getId().toString(), "SUCCESS", Map.of()));
        return new MintedToken(tokenValue, ttlSeconds, String.join(" ", client.getScopes()));
    }

    public record MintedToken(String accessToken, long expiresInSeconds, String scope) {
    }
}
