-- IDTrust E12 - Trust Registry (EPIC 13, FR-TRUST-*) core tables.
--
-- trust_registries matches the Baseline's section 7 ERD TENANT --< TRUST_REGISTRY relationship: one
-- tenant may operate several named registries (e.g. per trust framework). Unlike
-- credential_schemas/verification_policies, there is no version column - FR-TRUST-001 has no
-- publish/freeze acceptance criteria, so a registry is usable for issuer registration immediately
-- upon creation (status defaults to ACTIVE). framework is a plain free-text column, a deliberate
-- V1-priority stub for FR-TRUST-008 (Trust Framework) rather than a dedicated typed entity.
--
-- trusted_issuers folds FR-TRUST-002 (Register Issuer) and FR-TRUST-003 (Register DID) into a
-- single row, mirroring the project's established FR-folding precedent (E10's FR-ISS-001/002).
-- issuer_did is a plain string, not a foreign key into dids - trust registries exist specifically
-- to declare trust in external issuers whose DIDs this platform never provisioned. Likewise
-- trust_registry_id is a plain UUID column (no FK-relationship object), matching the project's
-- established plain-UUID-foreign-key convention (e.g. wallet_credentials.wallet_id).
-- trusted_credential_types is nullable JSONB (a raw string array) - null/empty means "trusted for
-- every credential type", mirroring verification_policies.policy's trustedIssuerDids "empty is not
-- fail-closed" precedent. The (tenant_id, issuer_did) index backs both FR-TRUST-006 (Trust Lookup)
-- and FR-TRUST-007 (Trust Check).
CREATE TABLE trust_registries (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id     UUID NOT NULL REFERENCES tenants(id),
    name          VARCHAR(255) NOT NULL,
    framework     VARCHAR(255),
    status        VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_trust_registries_tenant_id ON trust_registries(tenant_id);

CREATE TABLE trusted_issuers (
    id                        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id                 UUID NOT NULL REFERENCES tenants(id),
    trust_registry_id         UUID NOT NULL REFERENCES trust_registries(id),
    issuer_did                VARCHAR(500) NOT NULL,
    organization              VARCHAR(255),
    trusted_credential_types  JSONB,
    status                    VARCHAR(30) NOT NULL DEFAULT 'TRUSTED',
    registered_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_trusted_issuers_tenant_registry ON trusted_issuers(tenant_id, trust_registry_id);
CREATE INDEX idx_trusted_issuers_tenant_issuer_did ON trusted_issuers(tenant_id, issuer_did);
