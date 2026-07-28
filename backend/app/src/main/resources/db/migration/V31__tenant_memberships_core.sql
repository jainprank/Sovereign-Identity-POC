-- IDTrust DB Specification gap: tenant_memberships - lets a user be granted access to more than
-- one tenant with a coarse per-tenant role, additive to (and independent of) the existing
-- users.tenant_id "home tenant" column and the fine-grained user_roles RBAC assignment table.
-- Deliberately not wired into TenantMembershipFilter/UserRepository in this pass: those remain the
-- single-home-tenant authentication path; this table only backs the standalone
-- membership-management API below.
CREATE TABLE tenant_memberships (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id    UUID NOT NULL REFERENCES tenants(id),
    user_id      UUID NOT NULL REFERENCES users(id),
    role         VARCHAR(30) NOT NULL,
    status       VARCHAR(30) NOT NULL,
    joined_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_tenant_memberships_tenant_user UNIQUE (tenant_id, user_id)
);

CREATE INDEX idx_tenant_memberships_tenant ON tenant_memberships(tenant_id);
CREATE INDEX idx_tenant_memberships_user ON tenant_memberships(user_id);
