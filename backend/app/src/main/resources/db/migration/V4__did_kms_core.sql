-- IDTrust E04/E05 - DID lifecycle + KMS/Key Management core schema.
--
-- Table ownership (Architecture & Implementation Baseline, section 7 "PostgreSQL Core Data
-- Model"): dids is owned by the did module, keys is owned by the kms module. Both are
-- tenant-owned and will be subject to Row Level Security (see V5__did_kms_row_level_security.sql).
--
-- dids.document stores the full DID Document as JSONB rather than a separate did_documents
-- table: the Baseline's own DDL keeps it inline and no requirement in the spec calls for
-- document version history, so a separate table would be speculative.
CREATE TABLE dids (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id     UUID NOT NULL REFERENCES tenants(id),
    did           VARCHAR(2048) NOT NULL,
    method        VARCHAR(30) NOT NULL,
    status        VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    document      JSONB NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (did)
);

CREATE INDEX idx_dids_tenant_id ON dids(tenant_id);

-- keys.did_id is a plain FK column (no JPA relationship is modeled on the Java side): the did
-- module already depends on kms (for signing), so a bidirectional entity reference here would
-- create a circular Gradle module dependency. The FK constraint still enforces referential
-- integrity at the database layer.
--
-- Columns beyond the Baseline's literal DDL (public_key, encrypted_private_key,
-- rotated_from_key_id, updated_at) are intentional extensions: the spec requires private keys to
-- be encrypted before persistence and describes rotation (FR-KMS-004) but its example DDL doesn't
-- carry the columns needed to actually implement either.
CREATE TABLE keys (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id                UUID NOT NULL REFERENCES tenants(id),
    did_id                   UUID REFERENCES dids(id),
    key_type                 VARCHAR(30) NOT NULL,
    algorithm                VARCHAR(30) NOT NULL,
    provider                 VARCHAR(30) NOT NULL,
    external_key_id          VARCHAR(255),
    public_key               TEXT NOT NULL,
    encrypted_private_key    TEXT,
    rotated_from_key_id      UUID REFERENCES keys(id),
    status                   VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at               TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_keys_tenant_id ON keys(tenant_id);
CREATE INDEX idx_keys_did_id ON keys(did_id);
