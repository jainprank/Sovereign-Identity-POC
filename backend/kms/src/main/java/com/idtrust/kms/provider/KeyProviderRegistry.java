package com.idtrust.kms.provider;

import com.idtrust.common.web.ApiErrorCode;
import com.idtrust.common.web.ApiException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Resolves the {@link KeyProvider} implementation for a given {@link KeyProviderType}. Only
 * {@link KeyProviderType#INTERNAL} has an implementation in this pass (FR-KMS-* - "internal
 * encrypted store first"); AWS_KMS/VAULT/HSM are reserved enum values for future providers, and
 * requesting them fails clearly rather than silently falling back to the internal store.
 */
@Component
public class KeyProviderRegistry {

    private final Map<KeyProviderType, KeyProvider> providersByType;

    public KeyProviderRegistry(List<KeyProvider> providers) {
        this.providersByType = providers.stream()
                .collect(Collectors.toUnmodifiableMap(KeyProvider::type, Function.identity()));
    }

    public KeyProvider get(KeyProviderType type) {
        KeyProvider provider = providersByType.get(type);
        if (provider == null) {
            throw new ApiException(ApiErrorCode.INVALID_REQUEST, "Key provider not available: " + type);
        }
        return provider;
    }
}
