-- IDTrust E07 - Row Level Security for credentials.
--
-- Same tenant-isolation pattern established in V3/V5/V7: the application sets the Postgres
-- session variable app.tenant_id on every borrowed connection (TenantConnectionProvider), and the
-- policy below fails closed (zero rows) when no tenant is bound.

ALTER TABLE credentials ENABLE ROW LEVEL SECURITY;
ALTER TABLE credentials FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_credentials ON credentials
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);
