package com.idtrust.identity.membership;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public final class MembershipDtos {

    private MembershipDtos() {
    }

    public record CreateRequest(
            @NotNull UUID userId,
            @NotNull MembershipRole role
    ) {
    }

    public record RoleChangeRequest(
            @NotNull MembershipRole role
    ) {
    }

    public record Response(
            UUID id,
            UUID tenantId,
            UUID userId,
            MembershipRole role,
            MembershipStatus status,
            Instant joinedAt
    ) {
        public static Response from(TenantMembership membership) {
            return new Response(
                    membership.getId(),
                    membership.getTenantId(),
                    membership.getUserId(),
                    membership.getRole(),
                    membership.getStatus(),
                    membership.getJoinedAt()
            );
        }
    }
}
