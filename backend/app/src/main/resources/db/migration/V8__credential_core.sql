-- IDTrust E07 - Credential Issuance core schema.
--
-- Table ownership (Architecture & Implementation Baseline, section 7 "PostgreSQL Core Data
-- Model"): credentials is owned by the credential module (FR-ISS-*). Tenant-owned, will be
-- subject to Row Level Security (see V9__credential_row_level_security.sql).
--
-- Deviations from the Baseline's literal DDL, following the same "documented extension" pattern
-- used in V4/V6:
--   * credential_jwt (TEXT NOT NULL) is added to persist the actual signed VC. The Baseline's
--     example DDL only stores credential_hash, but the signed artifact itself must be retrievable
--     for later delivery (E09 OID4VCI) and re-verification (E10 OID4VP) - a hash alone can't be
--     handed back to a wallet or verifier.
--   * updated_at is added for consistency with every other tenant-owned entity in this codebase.
--   * schema_id is NOT NULL (the Baseline's DDL allows it nullable): every credential issued in
--     this pass must reference a published schema so its claims can be validated (FR-ISS-004)
--     against a known claim catalogue.
--
-- issuer_did/subject_did are stored as the DID string (not a FK to dids.id): subject_did in
-- particular is frequently a DID the platform does not manage (a holder's own wallet DID), so it
-- cannot be a foreign key into the tenant-owned dids table.
CREATE TABLE credentials (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id         UUID NOT NULL REFERENCES tenants(id),
    schema_id         UUID NOT NULL REFERENCES credential_schemas(id),
    issuer_did        VARCHAR(2048) NOT NULL,
    subject_did       VARCHAR(2048),
    format            VARCHAR(30) NOT NULL,
    credential_jwt    TEXT NOT NULL,
    credential_hash   VARCHAR(255) NOT NULL,
    status            VARCHAR(30) NOT NULL DEFAULT 'ISSUED',
    issued_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at        TIMESTAMPTZ,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_credentials_tenant_id ON credentials(tenant_id);
CREATE INDEX idx_credentials_tenant_schema_id ON credentials(tenant_id, schema_id);
CREATE INDEX idx_credentials_subject_did ON credentials(subject_did);
