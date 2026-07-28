-- IDTrust gap-fill (DB spec) - hedera_networks.
--
-- FR-HED-014 (Multi-Network Configuration) is documented as out of scope for V1 in
-- HederaProperties's Javadoc: today the platform binds to exactly one network via
-- idtrust.hedera.* environment variables. This table is a global reference catalogue (no
-- tenant_id, no RLS - same pattern as `permissions` in V2/V3) recording the well-known public
-- endpoints for Hedera's named networks, letting an operator inspect/select a network without
-- hardcoding endpoint strings in application config. It does not yet replace HederaProperties as
-- the actual source of truth the SDK client binds to (HcsHederaAnchorPublisher#buildClient still
-- reads idtrust.hedera.network directly) - wiring runtime network *selection* through this table is
-- future work, consistent with FR-HED-014 remaining V1-out-of-scope.
CREATE TABLE hedera_networks (
    id                        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    network_name              VARCHAR(30) NOT NULL UNIQUE,
    mirror_node_url            VARCHAR(255) NOT NULL,
    consensus_node_endpoint    VARCHAR(255),
    is_active                  BOOLEAN NOT NULL DEFAULT true,
    created_at                 TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                 TIMESTAMPTZ NOT NULL DEFAULT now()
);

INSERT INTO hedera_networks (network_name, mirror_node_url, consensus_node_endpoint, is_active) VALUES
    ('testnet', 'https://testnet.mirrornode.hedera.com', 'https://testnet.hashio.io/api', true),
    ('mainnet', 'https://mainnet-public.mirrornode.hedera.com', 'https://mainnet.hashio.io/api', true),
    ('previewnet', 'https://previewnet.mirrornode.hedera.com', 'https://previewnet.hashio.io/api', false);
