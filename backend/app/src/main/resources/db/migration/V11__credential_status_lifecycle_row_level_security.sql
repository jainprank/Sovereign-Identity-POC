-- IDTrust E08 - Row Level Security for credential_status_events.
--
-- Same tenant-isolation pattern established in V3/V5/V7/V9: the application sets the Postgres
-- session variable app.tenant_id on every borrowed connection (TenantConnectionProvider), and the
-- policy below fails closed (zero rows) when no tenant is bound. credentials itself already has
-- RLS from V9 - adding the claims/status_reason columns in V10 requires no policy change there.

ALTER TABLE credential_status_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE credential_status_events FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_credential_status_events ON credential_status_events
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);
