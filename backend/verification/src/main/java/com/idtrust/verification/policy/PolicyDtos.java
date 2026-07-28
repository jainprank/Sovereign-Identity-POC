package com.idtrust.verification.policy;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class PolicyDtos {

    private PolicyDtos() {
    }

    public record RuleRequest(UUID schemaId, List<String> requiredClaims, List<String> trustedIssuerDids,
                               boolean requireActiveStatus, boolean requireNotExpired) {
        PolicyDefinition toDefinition() {
            return new PolicyDefinition(schemaId, requiredClaims, trustedIssuerDids, requireActiveStatus, requireNotExpired);
        }
    }

    public record CreateRequest(@NotBlank String name, RuleRequest rules) {
    }

    public record UpdateRequest(RuleRequest rules) {
    }

    public record NewVersionRequest(RuleRequest rules) {
    }

    public record Response(
            UUID id,
            UUID tenantId,
            String name,
            int version,
            PolicyDefinition rules,
            PolicyStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        public static Response from(VerificationPolicy policy) {
            return new Response(
                    policy.getId(),
                    policy.getTenantId(),
                    policy.getName(),
                    policy.getVersion(),
                    policy.getDefinition(),
                    policy.getStatus(),
                    policy.getCreatedAt(),
                    policy.getUpdatedAt()
            );
        }
    }
}
