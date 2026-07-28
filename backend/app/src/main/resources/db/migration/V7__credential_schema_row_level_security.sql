-- IDTrust E06 - Row Level Security for credential_schemas.
--
-- Same tenant-isolation pattern established in V3/V5: the application sets the Postgres session
-- variable app.tenant_id on every borrowed connection (TenantConnectionProvider), and the policy
-- below fails closed (zero rows) when no tenant is bound.

ALTER TABLE credential_schemas ENABLE ROW LEVEL SECURITY;
ALTER TABLE credential_schemas FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_credential_schemas ON credential_schemas
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);
