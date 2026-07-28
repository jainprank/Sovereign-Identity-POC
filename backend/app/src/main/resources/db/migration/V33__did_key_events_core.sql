-- IDTrust DB Specification gap: did_key_events - an append-only history of a managed key's
-- lifecycle (generation/rotation/deactivation/deletion), keyed by the DID it backs where known.
-- Complements keys.rotated_from_key_id (which only ever points one hop back) with a full,
-- queryable audit trail per DID.
CREATE TABLE did_key_events (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id        UUID NOT NULL REFERENCES tenants(id),
    did_id           UUID,
    key_id           UUID NOT NULL REFERENCES keys(id),
    previous_key_id  UUID,
    event_type       VARCHAR(30) NOT NULL,
    occurred_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_did_key_events_tenant ON did_key_events(tenant_id);
CREATE INDEX idx_did_key_events_did ON did_key_events(did_id);
CREATE INDEX idx_did_key_events_key ON did_key_events(key_id);
