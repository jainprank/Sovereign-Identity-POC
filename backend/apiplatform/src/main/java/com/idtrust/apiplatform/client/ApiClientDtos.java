package com.idtrust.apiplatform.client;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ApiClientDtos {

    private ApiClientDtos() {
    }

    public record CreateRequest(
            @NotBlank String name,
            @NotNull ApiClientType clientType,
            @NotEmpty List<String> scopes,
            Instant expiresAt
    ) {
    }

    /** {@code secret} is populated exactly once, at creation time - see {@link ApiClient}'s Javadoc. */
    public record CreateResponse(ApiClientResponse client, String secret) {
        public static CreateResponse from(ApiClientService.ApiClientCreationResult result) {
            String secret = result.oneTimeApiKey() != null ? result.oneTimeApiKey() : result.oneTimeClientSecret();
            return new CreateResponse(ApiClientResponse.from(result.client()), secret);
        }
    }

    public record ApiClientResponse(
            UUID id,
            String name,
            ApiClientType clientType,
            String clientId,
            List<String> scopes,
            ApiClientStatus status,
            Instant expiresAt,
            Instant lastUsedAt,
            Instant createdAt,
            Instant updatedAt
    ) {
        public static ApiClientResponse from(ApiClient client) {
            return new ApiClientResponse(
                    client.getId(),
                    client.getName(),
                    client.getClientType(),
                    client.getClientId(),
                    client.getScopes(),
                    client.getStatus(),
                    client.getExpiresAt(),
                    client.getLastUsedAt(),
                    client.getCreatedAt(),
                    client.getUpdatedAt()
            );
        }
    }
}
