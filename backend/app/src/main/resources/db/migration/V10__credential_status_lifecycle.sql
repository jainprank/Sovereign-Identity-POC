-- IDTrust E08 - Credential Status & Revocation schema (EPIC 7, FR-CRED-*).
--
-- credentials.claims is a documented extension beyond the Baseline's literal DDL (which only
-- carries credential_hash): renewal (FR-CRED-009) and reissuance (FR-CRED-010) both need to
-- re-sign a new credential from the original's claims, and decoding them back out of
-- credential_jwt would be a fragile detour around data the platform already had at issuance time.
-- Backfilled with an empty object default for the ALTER on any pre-existing rows, then the default
-- is dropped so every future insert must supply claims explicitly (mirrors standard "add NOT NULL
-- column to a populated table" migration hygiene).
ALTER TABLE credentials ADD COLUMN claims JSONB NOT NULL DEFAULT '{}'::jsonb;
ALTER TABLE credentials ALTER COLUMN claims DROP DEFAULT;

-- status_reason carries the reason for the *current* status (e.g. why a credential was suspended
-- or revoked) - nullable since an ISSUED credential in good standing has no reason to record.
ALTER TABLE credentials ADD COLUMN status_reason TEXT;

-- credential_status_events fulfils the CREDENTIAL --< CREDENTIAL_STATUS one-to-many relationship
-- in the Baseline's section 7 ERD (not modeled by section 8's flat credentials.status column,
-- which this schema already implements as of V8) - a queryable lifecycle history distinct from the
-- generic audit_events log, satisfying FR-CRED-013 (Lifecycle Audit) with domain-specific fields
-- (previous/new status, reason) rather than requiring callers to parse generic audit metadata.
CREATE TABLE credential_status_events (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id         UUID NOT NULL REFERENCES tenants(id),
    credential_id     UUID NOT NULL REFERENCES credentials(id),
    previous_status   VARCHAR(30) NOT NULL,
    new_status        VARCHAR(30) NOT NULL,
    reason            TEXT,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_credential_status_events_tenant_credential
    ON credential_status_events(tenant_id, credential_id);
