-- IDTrust E14 - API & Developer Platform (EPIC 14, FR-API-*) core tables.
--
-- api_clients: a tenant-owned machine identity, either an API_KEY or an OAUTH_CLIENT
-- (client.ApiClientType) sharing one table per the Baseline ERD's single API_CLIENT node.
-- secret_hash is a SHA-256 hash (auth.CredentialHasher) of the plaintext API key or OAuth client
-- secret, revealed to the caller exactly once at creation time and never stored in the clear.
CREATE TABLE api_clients (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id     UUID NOT NULL REFERENCES tenants(id),
    name          VARCHAR(255) NOT NULL,
    client_type   VARCHAR(30) NOT NULL,
    client_id     VARCHAR(255) NOT NULL,
    secret_hash   VARCHAR(255) NOT NULL,
    scopes        VARCHAR(500) NOT NULL,
    status        VARCHAR(30) NOT NULL,
    expires_at    TIMESTAMPTZ,
    last_used_at  TIMESTAMPTZ,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_api_clients_client_id UNIQUE (client_id)
);

CREATE INDEX idx_api_clients_tenant ON api_clients(tenant_id);

-- api_access_tokens: an opaque bearer token minted for an OAUTH_CLIENT via the client_credentials
-- grant (FR-API-002) - deliberately not a self-signed JWT, see auth.ApiAccessToken's Javadoc.
CREATE TABLE api_access_tokens (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id      UUID NOT NULL REFERENCES tenants(id),
    api_client_id  UUID NOT NULL REFERENCES api_clients(id),
    token_hash     VARCHAR(255) NOT NULL,
    expires_at     TIMESTAMPTZ NOT NULL,
    revoked        BOOLEAN NOT NULL DEFAULT false,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_api_access_tokens_token_hash UNIQUE (token_hash)
);

CREATE INDEX idx_api_access_tokens_tenant ON api_access_tokens(tenant_id);
CREATE INDEX idx_api_access_tokens_client ON api_access_tokens(api_client_id);

-- api_request_logs: one row per completed API request (FR-API-007), captured by
-- log.ApiRequestLoggingFilter. tenant_id/api_client_id are nullable - a request that fails
-- authentication, or hits a platform-level/permitted endpoint, never resolves either.
CREATE TABLE api_request_logs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID REFERENCES tenants(id),
    api_client_id   UUID REFERENCES api_clients(id),
    method          VARCHAR(10) NOT NULL,
    path            VARCHAR(500) NOT NULL,
    status_code     INT NOT NULL,
    duration_ms     BIGINT NOT NULL,
    correlation_id  VARCHAR(100),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_api_request_logs_tenant_created ON api_request_logs(tenant_id, created_at);

-- webhook_endpoints: a tenant-owned outbound webhook subscription (FR-API-008). encrypted_secret
-- is envelope-encrypted (kms.crypto.KeyEncryptionService), not hashed, since it must be
-- recoverable in plaintext to compute the outbound X-IDTrust-Signature HMAC on every delivery.
CREATE TABLE webhook_endpoints (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id         UUID NOT NULL REFERENCES tenants(id),
    url               VARCHAR(2048) NOT NULL,
    event_types       VARCHAR(500) NOT NULL,
    encrypted_secret  TEXT NOT NULL,
    active            BOOLEAN NOT NULL DEFAULT true,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_webhook_endpoints_tenant ON webhook_endpoints(tenant_id);

-- webhook_deliveries: one row per delivery attempt of a domain event to a single
-- webhook_endpoint (FR-API-008), re-attempted in place (not a new row) by
-- webhook.WebhookDeliveryService#retry (FR-API-009).
CREATE TABLE webhook_deliveries (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL REFERENCES tenants(id),
    webhook_endpoint_id UUID NOT NULL REFERENCES webhook_endpoints(id),
    event_type          VARCHAR(100) NOT NULL,
    resource_type       VARCHAR(50) NOT NULL,
    resource_id         UUID NOT NULL,
    payload             TEXT NOT NULL,
    status              VARCHAR(30) NOT NULL,
    attempts            INT NOT NULL DEFAULT 0,
    last_error          TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_webhook_deliveries_tenant_endpoint ON webhook_deliveries(tenant_id, webhook_endpoint_id);
CREATE INDEX idx_webhook_deliveries_tenant_status ON webhook_deliveries(tenant_id, status);
