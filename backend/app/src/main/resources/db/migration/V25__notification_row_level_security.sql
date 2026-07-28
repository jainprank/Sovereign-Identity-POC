-- IDTrust E15 - Row Level Security for notifications.
--
-- Same tenant-isolation pattern established in V3/V5/.../V21/V23. Unlike audit_records,
-- notifications always has a resolvable tenant_id (see V24's header comment), so this policy is
-- the plain fail-closed form with no NULL carve-out.
ALTER TABLE notifications ENABLE ROW LEVEL SECURITY;
ALTER TABLE notifications FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_notifications ON notifications
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);
