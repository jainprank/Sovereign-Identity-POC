package com.idtrust.credential.template;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public final class CredentialTemplateDtos {

    private CredentialTemplateDtos() {
    }

    public record CreateRequest(@NotNull UUID schemaId, @NotBlank String name, String description,
                                 Map<String, Object> defaultClaims, Integer defaultExpiresInDays) {
    }

    public record UpdateRequest(String description, Map<String, Object> defaultClaims, Integer defaultExpiresInDays) {
    }

    public record Response(
            UUID id,
            UUID tenantId,
            UUID schemaId,
            String name,
            String description,
            Map<String, Object> defaultClaims,
            Integer defaultExpiresInDays,
            Instant createdAt,
            Instant updatedAt
    ) {
        public static Response from(CredentialTemplate template) {
            return new Response(
                    template.getId(),
                    template.getTenantId(),
                    template.getSchemaId(),
                    template.getName(),
                    template.getDescription(),
                    template.getDefaultClaims(),
                    template.getDefaultExpiresInDays(),
                    template.getCreatedAt(),
                    template.getUpdatedAt()
            );
        }
    }
}
