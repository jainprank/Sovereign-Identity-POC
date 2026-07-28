package com.idtrust.identity.user;

import java.time.Instant;
import java.util.UUID;

public final class UserDtos {

    private UserDtos() {
    }

    public record Response(
            UUID id,
            UUID tenantId,
            String keycloakSubjectId,
            String email,
            String displayName,
            UserStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        public static Response from(AppUser user) {
            return new Response(
                    user.getId(),
                    user.getTenantId(),
                    user.getKeycloakSubjectId(),
                    user.getEmail(),
                    user.getDisplayName(),
                    user.getStatus(),
                    user.getCreatedAt(),
                    user.getUpdatedAt()
            );
        }
    }
}
