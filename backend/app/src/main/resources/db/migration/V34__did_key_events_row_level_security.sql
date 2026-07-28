-- Same tenant-isolation pattern established in V3/V5/.../V29/V31.
ALTER TABLE did_key_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE did_key_events FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_did_key_events ON did_key_events
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);
