-- IDTrust E13 - Row Level Security for hedera_anchors.
--
-- Same tenant-isolation pattern established in V3/V5/V7/V9/V11/V13/V15/V17: the application sets
-- the Postgres session variable app.tenant_id on every borrowed connection
-- (TenantConnectionProvider), and the policy below fails closed (zero rows) when no tenant is
-- bound.

ALTER TABLE hedera_anchors ENABLE ROW LEVEL SECURITY;
ALTER TABLE hedera_anchors FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_hedera_anchors ON hedera_anchors
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);
