-- Same tenant-isolation pattern established in V3/V5/.../V37.
ALTER TABLE trust_entry_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE trust_entry_events FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_trust_entry_events ON trust_entry_events
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);
