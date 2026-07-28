-- IDTrust gap-fill (DB spec) - credential_templates.
--
-- Distinct from credential_schemas: a schema defines the claim catalogue a credential must
-- validate against (FR-SCH-*); a template is a reusable, tenant-owned issuance preset for a given
-- schema - a default claim payload and a default validity period - so an issuer doesn't have to
-- resupply the same boilerplate claims on every issuance request. Referenced (not required) by
-- CredentialIssuanceService#issueFromTemplate; direct issue() calls that don't use a template are
-- unaffected.
CREATE TABLE credential_templates (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id               UUID NOT NULL REFERENCES tenants(id),
    schema_id               UUID NOT NULL REFERENCES credential_schemas(id),
    name                    VARCHAR(255) NOT NULL,
    description             TEXT,
    default_claims          JSONB NOT NULL DEFAULT '{}'::jsonb,
    default_expires_in_days INTEGER,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_credential_templates_tenant_name UNIQUE (tenant_id, name)
);

CREATE INDEX idx_credential_templates_tenant_id ON credential_templates(tenant_id);
CREATE INDEX idx_credential_templates_tenant_schema_id ON credential_templates(tenant_id, schema_id);
