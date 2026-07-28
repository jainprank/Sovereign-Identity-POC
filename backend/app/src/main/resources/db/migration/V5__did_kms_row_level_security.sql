-- IDTrust E04/E05 - Row Level Security for dids and keys.
--
-- Same tenant-isolation pattern established in V3__row_level_security.sql: the application sets
-- the Postgres session variable app.tenant_id on every borrowed connection (TenantConnectionProvider),
-- and every policy below fails closed (zero rows) when no tenant is bound.

ALTER TABLE dids ENABLE ROW LEVEL SECURITY;
ALTER TABLE dids FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_dids ON dids
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);

ALTER TABLE keys ENABLE ROW LEVEL SECURITY;
ALTER TABLE keys FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_keys ON keys
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);
