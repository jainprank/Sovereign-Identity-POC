-- IDTrust E15 - Row Level Security for audit_records.
--
-- Same tenant-isolation pattern established in V3/V5/.../V21, with the same
-- "tenant_id IS NULL is allowed" carve-out V21 gave api_request_logs, for the identical reason:
-- a platform-level audit event (no resolvable tenant) must still be insertable/readable while no
-- tenant is bound to the borrowed connection.
ALTER TABLE audit_records ENABLE ROW LEVEL SECURITY;
ALTER TABLE audit_records FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_audit_records ON audit_records
    USING (tenant_id IS NULL OR tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id IS NULL OR tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);
