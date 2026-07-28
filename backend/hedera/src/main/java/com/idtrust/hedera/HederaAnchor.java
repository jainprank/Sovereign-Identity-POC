package com.idtrust.hedera;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * A single anchoring attempt of a platform resource's hash to Hedera Consensus Service (EPIC 16,
 * FR-HED-004 through 012). One row per {@code HederaAnchorPublisher.anchor(...)} call - never
 * updated in place to a different resource, only ever transitioned {@code FAILED -> CONFIRMED} via
 * {@link #markConfirmed} on retry (FR-HED-010).
 *
 * <p>Only {@code resourceHash} is ever sent to Hedera; this row (like every table in this
 * codebase) may additionally carry {@code tenantId} for RLS, which is never published on-chain -
 * see {@code com.idtrust.common.hedera.HederaAnchorRequest}'s Javadoc.
 */
@Entity
@Table(name = "hedera_anchors")
public class HederaAnchor {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "resource_type", nullable = false)
    private String resourceType;

    @Column(name = "resource_id", nullable = false)
    private UUID resourceId;

    @Column(name = "resource_hash", nullable = false)
    private String resourceHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AnchorStatus status;

    @Column(name = "transaction_id")
    private String transactionId;

    @Column(name = "topic_sequence_number")
    private Long topicSequenceNumber;

    @Column(name = "consensus_timestamp")
    private Instant consensusTimestamp;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected HederaAnchor() {
        // JPA
    }

    public HederaAnchor(UUID tenantId, String eventType, String resourceType, UUID resourceId, String resourceHash) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.eventType = eventType;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.resourceHash = resourceHash;
        this.status = AnchorStatus.FAILED;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    /** FR-HED-009/012 - a successful submission; consensus was reached on Hedera. */
    public void markConfirmed(String transactionId, Long topicSequenceNumber, Instant consensusTimestamp) {
        this.status = AnchorStatus.CONFIRMED;
        this.transactionId = transactionId;
        this.topicSequenceNumber = topicSequenceNumber;
        this.consensusTimestamp = consensusTimestamp;
        this.failureReason = null;
        this.updatedAt = Instant.now();
    }

    /** FR-HED-010 - submission was attempted but failed; retryable. */
    public void markFailed(String reason) {
        this.status = AnchorStatus.FAILED;
        this.failureReason = reason;
        this.updatedAt = Instant.now();
    }

    /** {@code idtrust.hedera.enabled=false} - no network call was attempted. */
    public void markDisabled() {
        this.status = AnchorStatus.DISABLED;
        this.failureReason = null;
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getResourceType() {
        return resourceType;
    }

    public UUID getResourceId() {
        return resourceId;
    }

    public String getResourceHash() {
        return resourceHash;
    }

    public AnchorStatus getStatus() {
        return status;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public Long getTopicSequenceNumber() {
        return topicSequenceNumber;
    }

    public Instant getConsensusTimestamp() {
        return consensusTimestamp;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
