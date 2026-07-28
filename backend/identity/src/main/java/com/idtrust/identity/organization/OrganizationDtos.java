package com.idtrust.identity.organization;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.UUID;

public final class OrganizationDtos {

    private OrganizationDtos() {
    }

    public record CreateRequest(
            @NotBlank String name,
            String description,
            String contactEmail
    ) {
    }

    public record UpdateRequest(
            String name,
            String description,
            String logoUrl,
            String website,
            String contactEmail
    ) {
    }

    public record Response(
            UUID id,
            String name,
            String description,
            OrganizationStatus status,
            String logoUrl,
            String website,
            String contactEmail,
            Instant createdAt,
            Instant updatedAt
    ) {
        public static Response from(Organization organization) {
            return new Response(
                    organization.getId(),
                    organization.getName(),
                    organization.getDescription(),
                    organization.getStatus(),
                    organization.getLogoUrl(),
                    organization.getWebsite(),
                    organization.getContactEmail(),
                    organization.getCreatedAt(),
                    organization.getUpdatedAt()
            );
        }
    }
}
