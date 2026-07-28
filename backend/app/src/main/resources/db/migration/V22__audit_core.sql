-- IDTrust E15 - Audit & Compliance (EPIC 15, FR-AUD-*) durable audit trail.
--
-- audit_records: the persisted form of every audit.AuditEventPublisher#publish call across the
-- whole codebase (FR-AUD-001 through 008 - Authentication/Authorization/DID/Key/Credential/
-- Issuance/Verification/Trust Audit - are already satisfied by the publish() call sites each of
-- those epics added; this table is what makes them searchable and exportable, FR-AUD-009/010).
-- tenant_id/actor_user_id/resource_id are nullable for the same reason api_request_logs' are:
-- a platform-level action (e.g. a login failure for an unknown email) never resolves any of them.
CREATE TABLE audit_records (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id      UUID,
    actor_user_id  UUID,
    action         VARCHAR(100) NOT NULL,
    resource_type  VARCHAR(50) NOT NULL,
    resource_id    VARCHAR(255),
    result         VARCHAR(30) NOT NULL,
    metadata       JSONB NOT NULL DEFAULT '{}'::jsonb,
    occurred_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_records_tenant_occurred ON audit_records(tenant_id, occurred_at);
CREATE INDEX idx_audit_records_action ON audit_records(action);
CREATE INDEX idx_audit_records_resource ON audit_records(resource_type, resource_id);
