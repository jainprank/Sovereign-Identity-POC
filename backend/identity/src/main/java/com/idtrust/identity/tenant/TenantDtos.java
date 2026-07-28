package com.idtrust.identity.tenant;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public final class TenantDtos {

    private TenantDtos() {
    }

    public record CreateRequest(@NotBlank String name) {
    }

    public record UpdateRequest(String name) {
    }

    public record ConfigurationRequest(Map<String, Object> configuration) {
    }

    public record Response(
            UUID id,
            UUID organizationId,
            String name,
            TenantStatus status,
            Map<String, Object> configuration,
            Instant createdAt,
            Instant updatedAt
    ) {
        public static Response from(Tenant tenant) {
            return new Response(
                    tenant.getId(),
                    tenant.getOrganizationId(),
                    tenant.getName(),
                    tenant.getStatus(),
                    tenant.getConfiguration(),
                    tenant.getCreatedAt(),
                    tenant.getUpdatedAt()
            );
        }
    }
}
