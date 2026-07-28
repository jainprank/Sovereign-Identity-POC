package com.idtrust.apiplatform.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * An opaque bearer token minted by {@code OAuthTokenService} for an {@code OAUTH_CLIENT}-type
 * {@link com.idtrust.apiplatform.client.ApiClient} (FR-API-002, client_credentials grant).
 * Deliberately not a self-contained signed JWT: this platform has no existing JWT-signing
 * dependency wired into any module outside {@code kms}'s DID/credential-signing pipeline, and an
 * opaque, hashed, server-side-verified token reuses exactly the same lookup-by-hash mechanism
 * already needed for {@code API_KEY} clients (see {@code ApiClientCredentialAuthenticator}) rather
 * than introducing a second, JWT-specific verification path.
 */
@Entity
@Table(name = "api_access_tokens")
public class ApiAccessToken {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "api_client_id", nullable = false)
    private UUID apiClientId;

    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean revoked;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ApiAccessToken() {
        // JPA
    }

    public ApiAccessToken(UUID tenantId, UUID apiClientId, String tokenHash, Instant expiresAt) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.apiClientId = apiClientId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.revoked = false;
        this.createdAt = Instant.now();
    }

    public boolean isValid() {
        return !revoked && expiresAt.isAfter(Instant.now());
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getApiClientId() {
        return apiClientId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public boolean isRevoked() {
        return revoked;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
