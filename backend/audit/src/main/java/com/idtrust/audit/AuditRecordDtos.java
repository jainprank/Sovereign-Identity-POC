package com.idtrust.audit;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public final class AuditRecordDtos {

    private AuditRecordDtos() {
    }

    public record AuditRecordResponse(UUID id, UUID tenantId, UUID actorUserId, String action, String resourceType,
                                       String resourceId, String result, Map<String, Object> metadata,
                                       Instant occurredAt) {

        public static AuditRecordResponse from(AuditRecord record) {
            return new AuditRecordResponse(record.getId(), record.getTenantId(), record.getActorUserId(),
                    record.getAction(), record.getResourceType(), record.getResourceId(), record.getResult(),
                    record.getMetadata(), record.getOccurredAt());
        }
    }
}
