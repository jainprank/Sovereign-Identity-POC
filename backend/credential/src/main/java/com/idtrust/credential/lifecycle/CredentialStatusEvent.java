package com.idtrust.credential.lifecycle;

import com.idtrust.credential.issuance.CredentialStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * A single lifecycle status transition for a {@code Credential} (FR-CRED-013 - Lifecycle Audit).
 * Fulfils the {@code CREDENTIAL --< CREDENTIAL_STATUS} one-to-many relationship shown in the
 * Baseline's section 7 ERD, which the flat {@code credentials.status} column (implemented in E07
 * per section 8's literal DDL) does not itself model. Distinct from the generic
 * {@code com.idtrust.common.audit.AuditEvent} log: this is a queryable, domain-typed history
 * (previous/new status, reason) rather than free-form audit metadata.
 */
@Entity
@Table(name = "credential_status_events")
public class CredentialStatusEvent {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "credential_id", nullable = false)
    private UUID credentialId;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", nullable = false)
    private CredentialStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false)
    private CredentialStatus newStatus;

    @Column
    private String reason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected CredentialStatusEvent() {
        // JPA
    }

    public CredentialStatusEvent(UUID tenantId, UUID credentialId, CredentialStatus previousStatus,
                                  CredentialStatus newStatus, String reason) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.credentialId = credentialId;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.reason = reason;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getCredentialId() {
        return credentialId;
    }

    public CredentialStatus getPreviousStatus() {
        return previousStatus;
    }

    public CredentialStatus getNewStatus() {
        return newStatus;
    }

    public String getReason() {
        return reason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
