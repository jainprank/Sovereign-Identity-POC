-- IDTrust E09 - Row Level Security for wallets, wallet_credentials and oid4vci_sessions.
--
-- Same tenant-isolation pattern established in V3/V5/V7/V9/V11: the application sets the Postgres
-- session variable app.tenant_id on every borrowed connection (TenantConnectionProvider), and the
-- policy below fails closed (zero rows) when no tenant is bound. Unlike every prior RLS-protected
-- table, some of these rows are read/written by unauthenticated OID4VCI protocol calls (a wallet
-- holder is not a platform Keycloak principal) - TenantContext is still populated for those calls
-- from the X-Tenant-ID header alone (TenantContextInterceptor runs for every request regardless of
-- authentication), so RLS enforcement is unaffected.

ALTER TABLE wallets ENABLE ROW LEVEL SECURITY;
ALTER TABLE wallets FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_wallets ON wallets
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);

ALTER TABLE wallet_credentials ENABLE ROW LEVEL SECURITY;
ALTER TABLE wallet_credentials FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_wallet_credentials ON wallet_credentials
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);

ALTER TABLE oid4vci_sessions ENABLE ROW LEVEL SECURITY;
ALTER TABLE oid4vci_sessions FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_oid4vci_sessions ON oid4vci_sessions
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);
