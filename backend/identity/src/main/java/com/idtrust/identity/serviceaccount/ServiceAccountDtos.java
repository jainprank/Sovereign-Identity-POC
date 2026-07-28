package com.idtrust.identity.serviceaccount;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.UUID;

public final class ServiceAccountDtos {

    private ServiceAccountDtos() {
    }

    public record CreateRequest(
            @NotBlank String name,
            String description
    ) {
    }

    public record Response(
            UUID id,
            UUID tenantId,
            String name,
            String description,
            ServiceAccountStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        public static Response from(ServiceAccount serviceAccount) {
            return new Response(
                    serviceAccount.getId(),
                    serviceAccount.getTenantId(),
                    serviceAccount.getName(),
                    serviceAccount.getDescription(),
                    serviceAccount.getStatus(),
                    serviceAccount.getCreatedAt(),
                    serviceAccount.getUpdatedAt()
            );
        }
    }
}
