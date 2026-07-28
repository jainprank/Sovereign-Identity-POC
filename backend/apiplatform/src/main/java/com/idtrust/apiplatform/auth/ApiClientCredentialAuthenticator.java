package com.idtrust.apiplatform.auth;

import com.idtrust.apiplatform.client.ApiClient;
import com.idtrust.apiplatform.client.ApiClientRepository;
import com.idtrust.apiplatform.client.ApiClientType;
import com.idtrust.common.security.ApiCredentialAuthenticator;
import com.idtrust.common.security.ApiPrincipal;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * The single real {@link ApiCredentialAuthenticator} implementation (see that interface's Javadoc
 * for why it lives in {@code common} while this implementation lives here). Tries an API key
 * lookup first, then an OAuth access token lookup - both are plain SHA-256-hash equality lookups
 * (see {@link CredentialHasher}), so trying both costs one extra indexed query at most.
 */
@Component
public class ApiClientCredentialAuthenticator implements ApiCredentialAuthenticator {

    private final ApiClientRepository apiClientRepository;
    private final ApiAccessTokenRepository accessTokenRepository;

    public ApiClientCredentialAuthenticator(ApiClientRepository apiClientRepository,
                                             ApiAccessTokenRepository accessTokenRepository) {
        this.apiClientRepository = apiClientRepository;
        this.accessTokenRepository = accessTokenRepository;
    }

    @Override
    @Transactional
    public Optional<ApiPrincipal> authenticate(String bearerToken) {
        if (bearerToken == null || bearerToken.isBlank()) {
            return Optional.empty();
        }
        String hash = CredentialHasher.hash(bearerToken);

        Optional<ApiClient> apiKeyClient = apiClientRepository.findBySecretHash(hash)
                .filter(client -> client.getClientType() == ApiClientType.API_KEY);
        if (apiKeyClient.isPresent()) {
            ApiClient client = apiKeyClient.get();
            if (!client.isUsable()) {
                return Optional.empty();
            }
            client.recordUsage();
            apiClientRepository.save(client);
            return Optional.of(new ApiPrincipal(client.getTenantId(), client.getId(), client.getScopes()));
        }

        return accessTokenRepository.findByTokenHash(hash)
                .filter(ApiAccessToken::isValid)
                .flatMap(token -> apiClientRepository.findById(token.getApiClientId())
                        .filter(ApiClient::isUsable)
                        .map(client -> new ApiPrincipal(token.getTenantId(), client.getId(), client.getScopes())));
    }
}
