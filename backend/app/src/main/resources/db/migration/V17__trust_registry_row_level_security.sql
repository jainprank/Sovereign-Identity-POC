-- IDTrust E12 - Row Level Security for trust_registries and trusted_issuers.
--
-- Same tenant-isolation pattern established in V3/V5/V7/V9/V11/V13/V15: the application sets the
-- Postgres session variable app.tenant_id on every borrowed connection (TenantConnectionProvider),
-- and the policy below fails closed (zero rows) when no tenant is bound.

ALTER TABLE trust_registries ENABLE ROW LEVEL SECURITY;
ALTER TABLE trust_registries FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_trust_registries ON trust_registries
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);

ALTER TABLE trusted_issuers ENABLE ROW LEVEL SECURITY;
ALTER TABLE trusted_issuers FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_trusted_issuers ON trusted_issuers
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);
