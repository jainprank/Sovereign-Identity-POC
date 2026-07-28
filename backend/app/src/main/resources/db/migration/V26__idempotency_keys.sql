-- IDTrust Hardened API alignment - Idempotency-Key support (Database Specification's
-- `idempotency_keys` table; Hardened API Specification's `Idempotency-Key` header parameter on
-- state-changing POST operations, cited in its `info.description` as a required design
-- principle).
--
-- idempotency_keys: one row per (tenant, client-supplied idempotency key, endpoint) triple. A
-- retried POST carrying the same key + endpoint + request body hash within the row's lifetime
-- gets the original response replayed instead of re-executing the operation; the same key against
-- a different request body is a client error (see IdempotencyFilter).
CREATE TABLE idempotency_keys (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id        UUID NOT NULL REFERENCES tenants(id),
    idempotency_key  VARCHAR(255) NOT NULL,
    endpoint         VARCHAR(255) NOT NULL,
    request_hash     VARCHAR(64) NOT NULL,
    response_status  INTEGER NOT NULL,
    response_body    TEXT,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at       TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_idempotency_keys_tenant_key_endpoint UNIQUE (tenant_id, idempotency_key, endpoint)
);

CREATE INDEX idx_idempotency_keys_expires_at ON idempotency_keys (expires_at);
