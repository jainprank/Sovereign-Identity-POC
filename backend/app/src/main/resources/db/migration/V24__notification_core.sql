-- IDTrust E15 - Notification (EPIC 17, FR-NOT-002) in-app notifications.
--
-- notifications: one row per (event, recipient user) pair, created by
-- notification.NotificationEventPublisherImpl for every active user of the event's tenant.
-- Unlike audit_records/api_request_logs, both tenant_id and user_id are always resolvable at
-- insert time (a NotificationEvent with no tenant is dropped before any row is written - see
-- NotificationEventPublisherImpl), so both are NOT NULL with a real FK, matching the standard
-- pattern from every prior epic's core-table migration.
CREATE TABLE notifications (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id      UUID NOT NULL REFERENCES tenants(id),
    user_id        UUID NOT NULL REFERENCES users(id),
    event_type     VARCHAR(100) NOT NULL,
    resource_type  VARCHAR(50) NOT NULL,
    resource_id    UUID,
    message        VARCHAR(1000) NOT NULL,
    read           BOOLEAN NOT NULL DEFAULT false,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_notifications_tenant_user_created ON notifications(tenant_id, user_id, created_at);
