-- Same tenant-isolation pattern established in V3/V5/.../V33.
ALTER TABLE credential_templates ENABLE ROW LEVEL SECURITY;
ALTER TABLE credential_templates FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_credential_templates ON credential_templates
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);
