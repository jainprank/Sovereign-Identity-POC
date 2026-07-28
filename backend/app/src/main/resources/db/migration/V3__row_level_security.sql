-- IDTrust E03 - Row Level Security for tenant-owned tables.
--
-- Enforces tenant isolation at the database layer (Architecture & Implementation Baseline,
-- section 23 "Tenant Isolation") in addition to the API layer (X-Tenant-ID header, see
-- TenantContextInterceptor) and the service layer. The application sets the Postgres session
-- variable app.tenant_id on every borrowed connection to the current request's tenant (see
-- TenantConnectionProvider); when no tenant is bound, the setting is empty and every policy
-- below fails closed (returns zero rows) rather than leaking data across tenants.
--
-- FORCE ROW LEVEL SECURITY applies the policy even to the table owner (the application's own
-- database role), which is required here since the app does not connect as a superuser.
-- organizations/tenants are intentionally NOT covered: they are platform-level tables addressed
-- by cross-tenant administrators, not tenant-owned data.

ALTER TABLE users ENABLE ROW LEVEL SECURITY;
ALTER TABLE users FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_users ON users
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);

ALTER TABLE user_roles ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_roles FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_user_roles ON user_roles
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);

ALTER TABLE invitations ENABLE ROW LEVEL SECURITY;
ALTER TABLE invitations FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_invitations ON invitations
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);

-- roles is a mixed table: tenant_id IS NULL rows are global system roles visible to every
-- tenant (and to platform-admin requests with no tenant bound); tenant_id IS NOT NULL rows are
-- a tenant's own custom roles (FR-ORG-013) and require an exact tenant match.
ALTER TABLE roles ENABLE ROW LEVEL SECURITY;
ALTER TABLE roles FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_roles ON roles
    USING (tenant_id IS NULL OR tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id IS NULL OR tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);

-- permissions and role_permissions are global reference/catalogue data (no tenant_id column);
-- no RLS needed.
