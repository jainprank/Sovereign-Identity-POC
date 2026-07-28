-- IDTrust E09 - Digital Wallet (EPIC 8, FR-WAL-*) and OID4VCI (EPIC 9, FR-OID4VCI-*) core tables.
--
-- wallets/wallet_credentials fulfil the Baseline's section 7 ERD's WALLET/WALLET_CREDENTIAL
-- entities (not present in section 8's literal DDL, which only lists organizations/tenants/dids/
-- keys/credential_schemas/credentials/verification_policies). did_id/owner_user_id are plain UUID
-- columns with no JPA relationship, mirroring the keys.did_id precedent from V4 (did/kms/identity
-- are otherwise decoupled - wallet must not gain a Gradle dependency on identity just to reference
-- the owning user).
--
-- oid4vci_sessions replaces E07's placeholder CredentialOfferGenerator (issuer + credential_id
-- only, no claim mechanism) with a real pre-authorized-code grant: FR-OID4VCI-006 (Pre-Authorized
-- Code), FR-OID4VCI-009 (Token Endpoint), FR-OID4VCI-011 (Access Token Validation), FR-OID4VCI-014
-- (Issuance Session).

CREATE TABLE wallets (
    id             UUID PRIMARY KEY,
    tenant_id      UUID NOT NULL REFERENCES tenants(id),
    owner_user_id  UUID,
    did_id         UUID NOT NULL REFERENCES dids(id),
    did            VARCHAR(500) NOT NULL,
    label          VARCHAR(255),
    status         VARCHAR(30) NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_wallets_tenant_id ON wallets(tenant_id);

CREATE TABLE wallet_credentials (
    id             UUID PRIMARY KEY,
    tenant_id      UUID NOT NULL REFERENCES tenants(id),
    wallet_id      UUID NOT NULL REFERENCES wallets(id),
    credential_id  UUID NOT NULL REFERENCES credentials(id),
    category       VARCHAR(255),
    received_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (wallet_id, credential_id)
);

CREATE INDEX idx_wallet_credentials_tenant_wallet ON wallet_credentials(tenant_id, wallet_id);

CREATE TABLE oid4vci_sessions (
    id                    UUID PRIMARY KEY,
    tenant_id             UUID NOT NULL REFERENCES tenants(id),
    credential_id         UUID NOT NULL REFERENCES credentials(id),
    status                VARCHAR(30) NOT NULL,
    pre_authorized_code   VARCHAR(255) NOT NULL UNIQUE,
    access_token          VARCHAR(255) UNIQUE,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at            TIMESTAMPTZ NOT NULL,
    claimed_at            TIMESTAMPTZ
);

CREATE INDEX idx_oid4vci_sessions_tenant_credential ON oid4vci_sessions(tenant_id, credential_id);
