package com.idtrust.hedera;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * A global reference row (no {@code tenantId}, no RLS - same pattern as {@code Permission}) naming
 * one of Hedera's networks and its well-known public endpoints. See
 * {@code V41__hedera_networks_core.sql} for why this doesn't yet drive runtime network selection
 * (FR-HED-014 remains V1-out-of-scope; {@code HcsHederaAnchorPublisher} still binds to
 * {@code idtrust.hedera.network} directly).
 */
@Entity
@Table(name = "hedera_networks")
public class HederaNetwork {

    @Id
    private UUID id;

    @Column(name = "network_name", nullable = false, unique = true)
    private String networkName;

    @Column(name = "mirror_node_url", nullable = false)
    private String mirrorNodeUrl;

    @Column(name = "consensus_node_endpoint")
    private String consensusNodeEndpoint;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected HederaNetwork() {
        // JPA
    }

    public HederaNetwork(String networkName, String mirrorNodeUrl, String consensusNodeEndpoint, boolean active) {
        this.id = UUID.randomUUID();
        this.networkName = networkName;
        this.mirrorNodeUrl = mirrorNodeUrl;
        this.consensusNodeEndpoint = consensusNodeEndpoint;
        this.active = active;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void setActive(boolean active) {
        this.active = active;
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getNetworkName() {
        return networkName;
    }

    public String getMirrorNodeUrl() {
        return mirrorNodeUrl;
    }

    public String getConsensusNodeEndpoint() {
        return consensusNodeEndpoint;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
