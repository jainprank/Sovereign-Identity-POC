-- IDTrust E13 - Hedera Integration (EPIC 16, FR-HED-*) core table.
--
-- One row per HederaAnchorPublisher.anchor(...) call (FR-HED-004 through 008/011) - never a
-- foreign-keyed relationship into trust_registries/dids/credential_schemas/credentials, since a
-- single anchor may outlive a retry (FR-HED-010) and different resource types share this one
-- table (resource_type + resource_id name the anchored row, matching audit_events' precedent of
-- a plain string/UUID pair rather than a typed FK per resource kind).
--
-- Only resource_hash (never raw content, PII or private keys) is ever sent to Hedera - see
-- common.hedera.HederaAnchorRequest's Javadoc (Baseline section 26, Decision 4). status has no
-- transient PENDING/SUBMITTED value - the Hedera Java SDK's TopicMessageSubmitTransaction blocks
-- for a receipt, so the terminal CONFIRMED/FAILED/DISABLED outcome is already known by the time
-- HcsHederaAnchorPublisher.anchor(...) returns (FR-HED-009 Transaction Status). consensus_timestamp
-- on a CONFIRMED row satisfies FR-HED-008 (Timestamp Anchoring) without a separate column/table.
CREATE TABLE hedera_anchors (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id              UUID NOT NULL REFERENCES tenants(id),
    event_type             VARCHAR(100) NOT NULL,
    resource_type          VARCHAR(50) NOT NULL,
    resource_id            UUID NOT NULL,
    resource_hash          VARCHAR(100) NOT NULL,
    status                 VARCHAR(30) NOT NULL,
    transaction_id         VARCHAR(255),
    topic_sequence_number  BIGINT,
    consensus_timestamp    TIMESTAMPTZ,
    failure_reason         TEXT,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_hedera_anchors_tenant_resource ON hedera_anchors(tenant_id, resource_type, resource_id);
CREATE INDEX idx_hedera_anchors_tenant_status ON hedera_anchors(tenant_id, status);
