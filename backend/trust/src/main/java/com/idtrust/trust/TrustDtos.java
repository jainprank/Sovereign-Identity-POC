package com.idtrust.trust;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class TrustDtos {

    private TrustDtos() {
    }

    public record CreateRegistryRequest(@NotBlank String name, String framework) {
    }

    public record RegisterIssuerRequest(@NotBlank String issuerDid, String organization,
                                         List<String> trustedCredentialTypes) {
    }

    public record UpdateStatusRequest(@NotBlank String status) {
    }

    public record RegistryResponse(
            UUID id,
            UUID tenantId,
            String name,
            String framework,
            RegistryStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        public static RegistryResponse from(TrustRegistry registry) {
            return new RegistryResponse(
                    registry.getId(),
                    registry.getTenantId(),
                    registry.getName(),
                    registry.getFramework(),
                    registry.getStatus(),
                    registry.getCreatedAt(),
                    registry.getUpdatedAt()
            );
        }
    }

    public record IssuerResponse(
            UUID id,
            UUID tenantId,
            UUID trustRegistryId,
            String issuerDid,
            String organization,
            List<String> trustedCredentialTypes,
            TrustStatus status,
            Instant registeredAt,
            Instant updatedAt
    ) {
        public static IssuerResponse from(TrustedIssuer issuer) {
            return new IssuerResponse(
                    issuer.getId(),
                    issuer.getTenantId(),
                    issuer.getTrustRegistryId(),
                    issuer.getIssuerDid(),
                    issuer.getOrganization(),
                    issuer.getTrustedCredentialTypes(),
                    issuer.getStatus(),
                    issuer.getRegisteredAt(),
                    issuer.getUpdatedAt()
            );
        }
    }
}
