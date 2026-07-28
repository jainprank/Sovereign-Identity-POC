package com.idtrust.did.provider;

import com.idtrust.common.web.ApiErrorCode;
import com.idtrust.common.web.ApiException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Resolves the {@link DidMethodProvider} implementation for a given {@link DidMethod}. */
@Component
public class DidMethodProviderRegistry {

    private final Map<DidMethod, DidMethodProvider> providersByMethod;

    public DidMethodProviderRegistry(List<DidMethodProvider> providers) {
        this.providersByMethod = providers.stream()
                .collect(Collectors.toUnmodifiableMap(DidMethodProvider::method, Function.identity()));
    }

    public DidMethodProvider get(DidMethod method) {
        DidMethodProvider provider = providersByMethod.get(method);
        if (provider == null) {
            throw new ApiException(ApiErrorCode.INVALID_REQUEST, "DID method not supported: " + method);
        }
        return provider;
    }
}
