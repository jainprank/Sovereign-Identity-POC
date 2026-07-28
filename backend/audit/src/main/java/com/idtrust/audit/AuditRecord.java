package com.idtrust.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * The durable, queryable form of a {@link com.idtrust.common.audit.AuditEvent} (FR-AUD-009/010).
 * Deliberately a 1:1 mirror of {@code AuditEvent}'s fields rather than a richer domain object -
 * this table is a write-once audit trail, not something the application ever updates.
 *
 * <p>Named {@code AuditRecord} (not {@code AuditEvent}) to avoid a same-name clash with the
 * already-established {@link com.idtrust.common.audit.AuditEvent} record every module publishes -
 * mirrors {@code identity.user.AppUser} avoiding a clash with {@code users}/{@code User}.
 */
@Entity
@Table(name = "audit_records")
public class AuditRecord {

    @Id
    private UUID id;

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "actor_user_id")
    private UUID actorUserId;

    @Column(nullable = false)
    private String action;

    @Column(name = "resource_type", nullable = false)
    private String resourceType;

    @Column(name = "resource_id")
    private String resourceId;

    @Column(nullable = false)
    private String result;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private Map<String, Object> metadata;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected AuditRecord() {
        // JPA
    }

    public AuditRecord(UUID tenantId, UUID actorUserId, String action, String resourceType, String resourceId,
                        String result, Map<String, Object> metadata, Instant occurredAt) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.actorUserId = actorUserId;
        this.action = action;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.result = result;
        this.metadata = metadata == null ? Map.of() : metadata;
        this.occurredAt = occurredAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getActorUserId() {
        return actorUserId;
    }

    public String getAction() {
        return action;
    }

    public String getResourceType() {
        return resourceType;
    }

    public String getResourceId() {
        return resourceId;
    }

    public String getResult() {
        return result;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
