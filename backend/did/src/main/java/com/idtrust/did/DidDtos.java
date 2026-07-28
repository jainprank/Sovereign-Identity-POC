package com.idtrust.did;

import com.idtrust.did.provider.DidMethod;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public final class DidDtos {

    private DidDtos() {
    }

    public record CreateRequest(@NotNull DidMethod method, String domain, String path) {
    }

    public record ResolveRequest(@NotNull String did) {
    }

    public record Response(
            UUID id,
            UUID tenantId,
            String did,
            DidMethod method,
            DidStatus status,
            Map<String, Object> document,
            Instant createdAt,
            Instant updatedAt
    ) {
        public static Response from(Did did) {
            return new Response(
                    did.getId(),
                    did.getTenantId(),
                    did.getDid(),
                    did.getMethod(),
                    did.getStatus(),
                    did.getDocument(),
                    did.getCreatedAt(),
                    did.getUpdatedAt()
            );
        }
    }
}
