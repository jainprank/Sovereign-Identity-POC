-- IDTrust DB Specification gap: service_accounts - a tenant-owned, non-human identity record
-- (e.g. an automation/system actor) distinct from api_clients (apiplatform module), which models
-- the *credential* used to authenticate as a machine caller (API key / OAuth client), not the
-- identity itself. A service account groups activity under a durable, human-readable identity
-- that can outlive any individual credential rotation.
CREATE TABLE service_accounts (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id    UUID NOT NULL REFERENCES tenants(id),
    name         VARCHAR(255) NOT NULL,
    description  VARCHAR(1000),
    status       VARCHAR(30) NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_service_accounts_tenant_name UNIQUE (tenant_id, name)
);

CREATE INDEX idx_service_accounts_tenant ON service_accounts(tenant_id);
