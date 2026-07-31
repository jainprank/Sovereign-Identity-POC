package com.idtrust.kms.key;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * One row per {@link ManagedKey} lifecycle transition (generation/rotation/deactivation/deletion),
 * recorded by {@link KeyService} alongside its existing {@code AuditEventPublisher} call - see
 * {@code V33__did_key_events_core.sql} for why this exists in addition to the single-hop
 * {@code keys.rotated_from_key_id} column.
 */
@Entity
@Table(name = "did_key_events")
public class DidKeyEvent {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "did_id")
    private UUID didId;

    // Nullable: ON DELETE SET NULL once the key itself is hard-deleted (see V42 migration) -
    // this row's history stays queryable by didId even after the key it refers to is gone.
    @Column(name = "key_id")
    private UUID keyId;

    @Column(name = "previous_key_id")
    private UUID previousKeyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private DidKeyEventType eventType;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected DidKeyEvent() {
        // JPA
    }

    public DidKeyEvent(UUID tenantId, UUID didId, UUID keyId, UUID previousKeyId, DidKeyEventType eventType) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.didId = didId;
        this.keyId = keyId;
        this.previousKeyId = previousKeyId;
        this.eventType = eventType;
        this.occurredAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getDidId() {
        return didId;
    }

    public UUID getKeyId() {
        return keyId;
    }

    public UUID getPreviousKeyId() {
        return previousKeyId;
    }

    public DidKeyEventType getEventType() {
        return eventType;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
