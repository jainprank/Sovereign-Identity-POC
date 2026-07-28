-- IDTrust gap-fill (DB spec) - trust_entry_events.
--
-- Mirrors did_key_events (V33): trusted_issuers only stores the *current* status; this is a
-- queryable append-only lifecycle trail (register/status-update/revoke) for FR-TRUST-011 (Trust
-- Audit), distinct from the generic audit_events log which TrustRegistryService already publishes
-- to alongside this table.
CREATE TABLE trust_entry_events (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL REFERENCES tenants(id),
    trusted_issuer_id   UUID NOT NULL REFERENCES trusted_issuers(id),
    previous_status     VARCHAR(30),
    new_status          VARCHAR(30) NOT NULL,
    reason              TEXT,
    occurred_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_trust_entry_events_tenant_issuer ON trust_entry_events(tenant_id, trusted_issuer_id);
