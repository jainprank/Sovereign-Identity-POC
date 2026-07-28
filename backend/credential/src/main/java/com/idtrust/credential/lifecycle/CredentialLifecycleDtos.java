package com.idtrust.credential.lifecycle;

import com.idtrust.credential.issuance.CredentialStatus;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public final class CredentialLifecycleDtos {

    private CredentialLifecycleDtos() {
    }

    public record RevokeRequest(String reason) {
    }

    public record SuspendRequest(String reason) {
    }

    public record RenewRequest(Instant expiresAt) {
    }

    public record ReissueRequest(Map<String, Object> claims, Instant expiresAt) {
    }

    public record StatusResponse(UUID credentialId, CredentialStatus status) {
    }

    public record HistoryEntryResponse(
            UUID id,
            UUID credentialId,
            CredentialStatus previousStatus,
            CredentialStatus newStatus,
            String reason,
            Instant createdAt
    ) {
        public static HistoryEntryResponse from(CredentialStatusEvent event) {
            return new HistoryEntryResponse(
                    event.getId(),
                    event.getCredentialId(),
                    event.getPreviousStatus(),
                    event.getNewStatus(),
                    event.getReason(),
                    event.getCreatedAt()
            );
        }
    }
}
