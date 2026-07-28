package com.idtrust.identity.invitation;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public final class InvitationDtos {

    private InvitationDtos() {
    }

    public record CreateRequest(@NotBlank @Email String email, @NotNull UUID roleId) {
    }

    public record AcceptRequest(@NotBlank String token, @NotBlank String displayName, String keycloakSubjectId) {
    }

    public record Response(
            UUID id,
            UUID tenantId,
            String email,
            UUID roleId,
            String token,
            InvitationStatus status,
            Instant expiresAt,
            Instant createdAt,
            Instant acceptedAt
    ) {
        public static Response from(Invitation invitation) {
            return new Response(
                    invitation.getId(),
                    invitation.getTenantId(),
                    invitation.getEmail(),
                    invitation.getRoleId(),
                    invitation.getToken(),
                    invitation.getStatus(),
                    invitation.getExpiresAt(),
                    invitation.getCreatedAt(),
                    invitation.getAcceptedAt()
            );
        }
    }
}
