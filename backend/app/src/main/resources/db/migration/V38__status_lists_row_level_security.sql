-- Same tenant-isolation pattern established in V3/V5/.../V35.
ALTER TABLE status_lists ENABLE ROW LEVEL SECURITY;
ALTER TABLE status_lists FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_status_lists ON status_lists
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);
