package com.idtrust.credential.schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class CredentialSchemaDtos {

    private CredentialSchemaDtos() {
    }

    public record CreateRequest(
            @NotBlank String name,
            @NotBlank String version,
            @NotNull CredentialFormat format,
            @NotEmpty List<ClaimDefinition> claims
    ) {
    }

    public record UpdateRequest(String name, List<ClaimDefinition> claims) {
    }

    public record NewVersionRequest(@NotBlank String version, List<ClaimDefinition> claims) {
    }

    public record Response(
            UUID id,
            UUID tenantId,
            String name,
            String version,
            CredentialFormat format,
            SchemaStatus status,
            List<ClaimDefinition> claims,
            Instant createdAt,
            Instant updatedAt
    ) {
        public static Response from(CredentialSchema schema) {
            return new Response(
                    schema.getId(),
                    schema.getTenantId(),
                    schema.getName(),
                    schema.getVersion(),
                    schema.getFormat(),
                    schema.getStatus(),
                    schema.getDefinition().claims(),
                    schema.getCreatedAt(),
                    schema.getUpdatedAt()
            );
        }
    }
}
