-- IDTrust E10 - Row Level Security for verification_policies, verification_requests and
-- wallet_presentations.
--
-- Same tenant-isolation pattern established in V3/V5/V7/V9/V11/V13: the application sets the
-- Postgres session variable app.tenant_id on every borrowed connection (TenantConnectionProvider),
-- and the policy below fails closed (zero rows) when no tenant is bound. Like V13's
-- oid4vci_sessions, verification_requests is written by an unauthenticated OID4VP protocol call
-- (a wallet holder presenting a credential is not a platform Keycloak principal) - TenantContext
-- is still populated for that call from the X-Tenant-ID header alone (TenantContextInterceptor
-- runs for every request regardless of authentication), so RLS enforcement is unaffected.

ALTER TABLE verification_policies ENABLE ROW LEVEL SECURITY;
ALTER TABLE verification_policies FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_verification_policies ON verification_policies
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);

ALTER TABLE verification_requests ENABLE ROW LEVEL SECURITY;
ALTER TABLE verification_requests FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_verification_requests ON verification_requests
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);

ALTER TABLE wallet_presentations ENABLE ROW LEVEL SECURITY;
ALTER TABLE wallet_presentations FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_wallet_presentations ON wallet_presentations
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);
