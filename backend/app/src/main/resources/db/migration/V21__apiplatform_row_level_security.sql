-- IDTrust E14 - Row Level Security for the API & Developer Platform tables.
--
-- Same tenant-isolation pattern established in V3/V5/V7/V9/V11/V13/V15/V17/V19: the application
-- sets the Postgres session variable app.tenant_id on every borrowed connection
-- (TenantConnectionProvider), and each policy below fails closed (zero rows) when no tenant is
-- bound. api_request_logs additionally allows tenant_id IS NULL rows (unauthenticated/platform-
-- level requests) to be inserted/read only while no tenant is bound to the connection - see
-- log.ApiRequestLoggingFilter, which runs before TenantContext may have been populated.

ALTER TABLE api_clients ENABLE ROW LEVEL SECURITY;
ALTER TABLE api_clients FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_api_clients ON api_clients
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);

ALTER TABLE api_access_tokens ENABLE ROW LEVEL SECURITY;
ALTER TABLE api_access_tokens FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_api_access_tokens ON api_access_tokens
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);

ALTER TABLE api_request_logs ENABLE ROW LEVEL SECURITY;
ALTER TABLE api_request_logs FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_api_request_logs ON api_request_logs
    USING (tenant_id IS NULL OR tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id IS NULL OR tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);

ALTER TABLE webhook_endpoints ENABLE ROW LEVEL SECURITY;
ALTER TABLE webhook_endpoints FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_webhook_endpoints ON webhook_endpoints
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);

ALTER TABLE webhook_deliveries ENABLE ROW LEVEL SECURITY;
ALTER TABLE webhook_deliveries FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_webhook_deliveries ON webhook_deliveries
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);
