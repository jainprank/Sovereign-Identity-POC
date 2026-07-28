package com.idtrust.apiplatform.client;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * A tenant-owned machine identity (FR-API-001 API Key / FR-API-002 OAuth Client) - see
 * {@link ApiClientType}'s Javadoc for why both share this one table. {@code secretHash} is a
 * SHA-256 hash (see {@code auth.CredentialHasher}) of either the API key's own plaintext value
 * ({@code API_KEY}) or the OAuth client secret ({@code OAUTH_CLIENT}) - never the raw value
 * itself, mirroring standard practice for any bearer credential that is only ever compared, never
 * used to produce further signed material (contrast {@code webhook.WebhookEndpoint.secret}, which
 * must be recoverable to compute outbound HMAC signatures and so is encrypted, not hashed).
 * {@code scopes} is a comma-separated subset of {@link ApiScopes#VALID_ROLE_CODES} - see that
 * class's Javadoc for why a plain string column is used instead of a join table.
 */
@Entity
@Table(name = "api_clients")
public class ApiClient {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "client_type", nullable = false)
    private ApiClientType clientType;

    @Column(name = "client_id", nullable = false, unique = true)
    private String clientId;

    @Column(name = "secret_hash", nullable = false)
    private String secretHash;

    @Column(nullable = false)
    private String scopes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApiClientStatus status;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ApiClient() {
        // JPA
    }

    public ApiClient(UUID tenantId, String name, ApiClientType clientType, String clientId, String secretHash,
                      List<String> scopes, Instant expiresAt) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.name = name;
        this.clientType = clientType;
        this.clientId = clientId;
        this.secretHash = secretHash;
        this.scopes = String.join(",", scopes);
        this.status = ApiClientStatus.ACTIVE;
        this.expiresAt = expiresAt;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void revoke() {
        this.status = ApiClientStatus.REVOKED;
        this.updatedAt = Instant.now();
    }

    public void recordUsage() {
        this.lastUsedAt = Instant.now();
    }

    public boolean isUsable() {
        if (status != ApiClientStatus.ACTIVE) {
            return false;
        }
        return expiresAt == null || expiresAt.isAfter(Instant.now());
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getName() {
        return name;
    }

    public ApiClientType getClientType() {
        return clientType;
    }

    public String getClientId() {
        return clientId;
    }

    public String getSecretHash() {
        return secretHash;
    }

    public List<String> getScopes() {
        return List.of(scopes.split(","));
    }

    public ApiClientStatus getStatus() {
        return status;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getLastUsedAt() {
        return lastUsedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
