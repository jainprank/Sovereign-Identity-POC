package com.idtrust.trust;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * One row per {@link TrustedIssuer} lifecycle transition (register/status-update/revoke),
 * recorded by {@link TrustRegistryService} alongside its existing {@code AuditEventPublisher} call -
 * mirrors {@code com.idtrust.kms.key.DidKeyEvent}'s precedent (see
 * {@code V39__trust_entry_events_core.sql}).
 */
@Entity
@Table(name = "trust_entry_events")
public class TrustEntryEvent {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "trusted_issuer_id", nullable = false)
    private UUID trustedIssuerId;

    @Column(name = "previous_status")
    private String previousStatus;

    @Column(name = "new_status", nullable = false)
    private String newStatus;

    @Column
    private String reason;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected TrustEntryEvent() {
        // JPA
    }

    public TrustEntryEvent(UUID tenantId, UUID trustedIssuerId, TrustStatus previousStatus, TrustStatus newStatus,
                            String reason) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.trustedIssuerId = trustedIssuerId;
        this.previousStatus = previousStatus != null ? previousStatus.name() : null;
        this.newStatus = newStatus.name();
        this.reason = reason;
        this.occurredAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getTrustedIssuerId() {
        return trustedIssuerId;
    }

    public String getPreviousStatus() {
        return previousStatus;
    }

    public String getNewStatus() {
        return newStatus;
    }

    public String getReason() {
        return reason;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
