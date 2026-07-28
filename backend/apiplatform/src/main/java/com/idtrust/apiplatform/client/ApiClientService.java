package com.idtrust.apiplatform.client;

import com.idtrust.apiplatform.auth.CredentialHasher;
import com.idtrust.common.audit.AuditEvent;
import com.idtrust.common.audit.AuditEventPublisher;
import com.idtrust.common.tenant.CurrentUserContext;
import com.idtrust.common.web.ApiErrorCode;
import com.idtrust.common.web.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Implements FR-API-001 (API Key) and the client-management half of FR-API-002 (OAuth Client) -
 * issuing the OAuth access token itself (once a client already exists) is
 * {@code auth.OAuthTokenService}'s job.
 */
@Service
public class ApiClientService {

    private final ApiClientRepository apiClientRepository;
    private final AuditEventPublisher auditEventPublisher;

    public ApiClientService(ApiClientRepository apiClientRepository, AuditEventPublisher auditEventPublisher) {
        this.apiClientRepository = apiClientRepository;
        this.auditEventPublisher = auditEventPublisher;
    }

    /**
     * Creates a new {@link ApiClient} and returns the one-time plaintext secret alongside it - the
     * only point in this credential's lifetime the plaintext value is ever available; only its
     * hash is persisted (see {@link ApiClient}'s Javadoc).
     */
    @Transactional
    public ApiClientCreationResult create(UUID tenantId, String name, ApiClientType clientType, List<String> scopes,
                                           Instant expiresAt) {
        ApiScopes.validate(scopes);
        String clientId = (clientType == ApiClientType.API_KEY ? "ak_" : "oc_") + UUID.randomUUID();
        String secret = CredentialHasher.generateSecret();
        ApiClient client = new ApiClient(tenantId, name, clientType, clientId, CredentialHasher.hash(secret), scopes, expiresAt);
        apiClientRepository.save(client);
        audit(tenantId, "API_CLIENT_CREATED", client.getId());

        // FR-API-001 - an API key is used directly as the bearer token; the OAuth client instead
        // authenticates to /api/v1/oauth/token with clientId+secret to mint a separate access token.
        String bearerValue = clientType == ApiClientType.API_KEY ? secret : null;
        return new ApiClientCreationResult(client, bearerValue, clientType == ApiClientType.OAUTH_CLIENT ? secret : null);
    }

    public ApiClient get(UUID tenantId, UUID id) {
        return apiClientRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new ApiException(ApiErrorCode.RESOURCE_NOT_FOUND, "API client not found: " + id));
    }

    public List<ApiClient> list(UUID tenantId) {
        return apiClientRepository.findByTenantId(tenantId);
    }

    @Transactional
    public ApiClient revoke(UUID tenantId, UUID id) {
        ApiClient client = get(tenantId, id);
        if (client.getStatus() != ApiClientStatus.ACTIVE) {
            throw new ApiException(ApiErrorCode.INVALID_REQUEST, "Only an ACTIVE API client can be revoked: " + id);
        }
        client.revoke();
        audit(tenantId, "API_CLIENT_REVOKED", client.getId());
        return client;
    }

    private void audit(UUID tenantId, String action, UUID clientId) {
        auditEventPublisher.publish(AuditEvent.of(
                tenantId, CurrentUserContext.getUserId(), action, "API_CLIENT", clientId.toString(), "SUCCESS", Map.of()));
    }

    /** @param oneTimeApiKey the plaintext API key value (only set for {@code API_KEY} clients) */
    public record ApiClientCreationResult(ApiClient client, String oneTimeApiKey, String oneTimeClientSecret) {
    }
}
