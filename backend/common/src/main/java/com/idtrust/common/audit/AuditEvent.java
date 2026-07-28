package com.idtrust.common.audit;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * A single audit trail entry. Every module is expected to emit one of these for state-changing
 * operations (FR-AUD-* in the requirements catalogue: authentication, DID, key, credential,
 * issuance, verification and trust events all require an audit record).
 *
 * @param tenantId      tenant the action was performed in/against (nullable for platform-level actions)
 * @param actorUserId   the authenticated user performing the action (nullable for system actions)
 * @param action        a stable action name, e.g. {@code "DID_CREATED"}, {@code "CREDENTIAL_REVOKED"}
 * @param resourceType  the type of resource acted on, e.g. {@code "DID"}, {@code "CREDENTIAL"}
 * @param resourceId    identifier of the resource acted on
 * @param result        {@code "SUCCESS"} or {@code "FAILURE"}
 * @param metadata      additional structured context (kept small; never place PII or secrets here)
 * @param occurredAt    when the action occurred
 */
public record AuditEvent(
        UUID tenantId,
        UUID actorUserId,
        String action,
        String resourceType,
        String resourceId,
        String result,
        Map<String, Object> metadata,
        Instant occurredAt
) {

    public static AuditEvent of(UUID tenantId, UUID actorUserId, String action, String resourceType,
                                 String resourceId, String result, Map<String, Object> metadata) {
        return new AuditEvent(tenantId, actorUserId, action, resourceType, resourceId, result, metadata, Instant.now());
    }
}
