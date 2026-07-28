-- IDTrust gap-fill (DB spec) - status_lists.
--
-- CredentialLifecycleService's class Javadoc already documents this gap explicitly: FR-CRED-012
-- (Bitstring Status List / W3C StatusList2021) is V1-priority and out of scope, so statusList()
-- instead returns a plain id/status list (FR-CRED-011). This migration adds the actual bitstring
-- storage so revocation can be represented as a StatusList2021-shaped compressed bitstring, scoped
-- to the REVOCATION purpose only (SUSPENSION continues to use the existing credentials.status
-- column, since suspension is reversible and StatusList2021 bitstrings model permanent flags best).
--
-- One list per (tenant, issuer_did, purpose) is allocated lazily on first use and filled by
-- allocating the next free bit index to each newly issued credential - see
-- credential.statuslist.StatusListService#allocate, called from CredentialIssuanceService#issue.
CREATE TABLE status_lists (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id         UUID NOT NULL REFERENCES tenants(id),
    issuer_did        VARCHAR(2048) NOT NULL,
    purpose           VARCHAR(30) NOT NULL,
    size              INTEGER NOT NULL,
    next_free_index   INTEGER NOT NULL DEFAULT 0,
    encoded_list      TEXT NOT NULL,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_status_lists_tenant_issuer_purpose UNIQUE (tenant_id, issuer_did, purpose)
);

CREATE INDEX idx_status_lists_tenant_id ON status_lists(tenant_id);

-- Points a credential at the bit that represents its revocation status. Nullable: only credentials
-- issued after this feature landed (and only for the REVOCATION purpose) have one assigned.
ALTER TABLE credentials ADD COLUMN status_list_id UUID REFERENCES status_lists(id);
ALTER TABLE credentials ADD COLUMN status_list_index INTEGER;
