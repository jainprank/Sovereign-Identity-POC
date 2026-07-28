-- IDTrust E06 - Credential Schemas core schema.
--
-- Table ownership (Architecture & Implementation Baseline, section 7 "PostgreSQL Core Data
-- Model"): credential_schemas is owned by the credential module (FR-SCH-*). Tenant-owned, will be
-- subject to Row Level Security (see V7__credential_schema_row_level_security.sql).
--
-- The Baseline's literal DDL carries a single "version" string per row with no version lineage
-- column; this schema follows that model - versioning a schema (FR-SCH-004) creates a new row
-- with the same name and a new version rather than mutating history, and the unique constraint on
-- (tenant_id, name, version) prevents accidental duplicate versions.
--
-- schema JSONB carries the full claim catalogue (name/type/required/description/validation/
-- display per claim - FR-SCH-007 through FR-SCH-011) rather than a normalized claims table: the
-- Baseline's own DDL keeps it inline, and no requirement calls for querying into individual claims.
--
-- updated_at is an intentional extension beyond the Baseline's literal DDL, for the same reason as
-- V4: draft schemas can be edited (FR-SCH-003) and transition through publish/deprecate
-- (FR-SCH-005/006).
CREATE TABLE credential_schemas (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id     UUID NOT NULL REFERENCES tenants(id),
    name          VARCHAR(255) NOT NULL,
    version       VARCHAR(50) NOT NULL,
    format        VARCHAR(30) NOT NULL,
    schema        JSONB NOT NULL,
    status        VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (tenant_id, name, version)
);

CREATE INDEX idx_credential_schemas_tenant_id ON credential_schemas(tenant_id);
CREATE INDEX idx_credential_schemas_tenant_name ON credential_schemas(tenant_id, name);
