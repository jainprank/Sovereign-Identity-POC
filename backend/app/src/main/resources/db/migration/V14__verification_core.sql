-- IDTrust E10 - Verification (EPIC 10, FR-VER-*), OID4VP (EPIC 11, FR-OID4VP-*) and the Policy
-- Engine (EPIC 12, FR-POL-*) core tables, plus the wallet-side presentation history table
-- (FR-WAL-011/012/017) added alongside them since both sides of the OID4VP handshake are built in
-- this same pass.
--
-- verification_policies matches the Baseline's section 8 literal DDL shape (id/tenant_id/name/
-- version/policy/status/created_at) more closely than credential_schemas does: version is an
-- INTEGER here (auto-incrementing per name, starting at 1), not a free-text string, per that DDL.
-- policy JSONB carries the full PolicyDefinition (schemaId/requiredClaims/trustedIssuerDids/
-- requireActiveStatus/requireNotExpired - FR-POL-006 through 011). updated_at is a documented
-- extension beyond the literal DDL, for the same reason as credential_schemas.updated_at: draft
-- policies can be edited (FR-POL-003) and transition through activate/deactivate (FR-POL-004/005).
--
-- verification_requests fulfils the Baseline's section 7 ERD's VERIFICATION_REQUEST entity
-- (FR-VER-001 through 003). presentation_definition/checks are JSONB rather than normalized
-- tables for the same reason credentials.claims is: nothing calls for querying into individual
-- check results, only for returning them whole (FR-VER-013). nonce is unique per request, backing
-- FR-OID4VP-012 (Replay Protection).
--
-- wallet_presentations fulfils FR-WAL-017 (Presentation History) - see wallet.oid4vp.
-- WalletPresentation's Javadoc for why the full VP JWT is stored here (unlike wallet_credentials,
-- which only stores a credential_id reference).
CREATE TABLE verification_policies (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id     UUID NOT NULL REFERENCES tenants(id),
    name          VARCHAR(255) NOT NULL,
    version       INTEGER NOT NULL,
    policy        JSONB NOT NULL,
    status        VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (tenant_id, name, version)
);

CREATE INDEX idx_verification_policies_tenant_id ON verification_policies(tenant_id);

CREATE TABLE verification_requests (
    id                        UUID PRIMARY KEY,
    tenant_id                 UUID NOT NULL REFERENCES tenants(id),
    policy_id                 UUID NOT NULL REFERENCES verification_policies(id),
    callback_url              VARCHAR(2048),
    nonce                     VARCHAR(255) NOT NULL UNIQUE,
    status                    VARCHAR(30) NOT NULL,
    presentation_definition   JSONB NOT NULL,
    checks                    JSONB,
    status_reason             VARCHAR(500),
    verified_credential_id    UUID REFERENCES credentials(id),
    verified_subject_did      VARCHAR(500),
    created_at                TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at                TIMESTAMPTZ NOT NULL,
    verified_at               TIMESTAMPTZ
);

CREATE INDEX idx_verification_requests_tenant_policy ON verification_requests(tenant_id, policy_id);
CREATE INDEX idx_verification_requests_tenant_status ON verification_requests(tenant_id, status);

CREATE TABLE wallet_presentations (
    id                     UUID PRIMARY KEY,
    tenant_id              UUID NOT NULL REFERENCES tenants(id),
    wallet_id              UUID NOT NULL REFERENCES wallets(id),
    wallet_credential_id   UUID NOT NULL REFERENCES wallet_credentials(id),
    audience               VARCHAR(500) NOT NULL,
    nonce                  VARCHAR(255) NOT NULL,
    presentation_jwt       TEXT NOT NULL,
    presented_at           TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_wallet_presentations_tenant_wallet ON wallet_presentations(tenant_id, wallet_id);
