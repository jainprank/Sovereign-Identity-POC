-- IDTrust E02/E03 - Organization/Tenant/User/RBAC core schema.
--
-- Table ownership (Architecture & Implementation Baseline, section 7 "PostgreSQL Core Data
-- Model" and Master Specification, section 23 "Database Architecture"):
--   organizations, tenants        - platform-level, not tenant-scoped themselves
--   users, roles (tenant-scoped), permissions (global), role_permissions, user_roles,
--   invitations                    - tenant-owned; carry tenant_id and are subject to Row Level
--                                    Security (see V3__row_level_security.sql)

CREATE TABLE organizations (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    status          VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    logo_url        TEXT,
    website         TEXT,
    contact_email   VARCHAR(320),
    metadata        JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE tenants (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id),
    name            VARCHAR(255) NOT NULL,
    status          VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    configuration   JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (organization_id, name)
);

CREATE INDEX idx_tenants_organization_id ON tenants(organization_id);

CREATE TABLE users (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id             UUID NOT NULL REFERENCES tenants(id),
    keycloak_subject_id   VARCHAR(255),
    email                 VARCHAR(320) NOT NULL,
    display_name          VARCHAR(255),
    status                VARCHAR(30) NOT NULL DEFAULT 'INVITED',
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (tenant_id, email)
);

CREATE INDEX idx_users_tenant_id ON users(tenant_id);
CREATE INDEX idx_users_keycloak_subject_id ON users(keycloak_subject_id);

-- Roles: tenant_id NULL denotes a platform-wide system role (seeded below); non-null denotes a
-- tenant's custom role (FR-ORG-011/FR-ORG-013).
CREATE TABLE roles (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id     UUID REFERENCES tenants(id),
    code          VARCHAR(100) NOT NULL,
    name          VARCHAR(255) NOT NULL,
    description   TEXT,
    is_system     BOOLEAN NOT NULL DEFAULT false,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_roles_global_code ON roles(code) WHERE tenant_id IS NULL;
CREATE UNIQUE INDEX uq_roles_tenant_code ON roles(tenant_id, code) WHERE tenant_id IS NOT NULL;

CREATE TABLE permissions (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    resource      VARCHAR(100) NOT NULL,
    action        VARCHAR(100) NOT NULL,
    description   TEXT,
    UNIQUE (resource, action)
);

CREATE TABLE role_permissions (
    role_id         UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id   UUID NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE user_roles (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id       UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    tenant_id     UUID NOT NULL REFERENCES tenants(id),
    assigned_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_id, role_id)
);

CREATE INDEX idx_user_roles_tenant_id ON user_roles(tenant_id);
CREATE INDEX idx_user_roles_user_id ON user_roles(user_id);

CREATE TABLE invitations (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id     UUID NOT NULL REFERENCES tenants(id),
    email         VARCHAR(320) NOT NULL,
    role_id       UUID NOT NULL REFERENCES roles(id),
    token         VARCHAR(255) NOT NULL UNIQUE,
    status        VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    expires_at    TIMESTAMPTZ NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    accepted_at   TIMESTAMPTZ
);

CREATE INDEX idx_invitations_tenant_id ON invitations(tenant_id);

-- Seed platform system roles (FR-ORG-006/011, Master Specification section 6 "User Personas").
-- These are global (tenant_id IS NULL) and visible to every tenant; tenants additionally define
-- their own custom roles (FR-ORG-013) with tenant_id set.
INSERT INTO roles (code, name, description, is_system) VALUES
    ('PLATFORM_ADMIN', 'Platform Administrator', 'Manages organizations, tenants and global platform configuration', true),
    ('ORG_ADMIN', 'Organization Administrator', 'Manages organization users, roles, DIDs, issuers, verifiers and policies', true),
    ('ISSUER', 'Credential Issuer', 'Manages credential schemas, templates, issuance and revocation', true),
    ('VERIFIER', 'Verifier', 'Requests credentials and defines verification policies', true),
    ('HOLDER', 'Credential Holder', 'Owns a digital identity and stores/presents credentials', true),
    ('DEVELOPER', 'Developer', 'Uses APIs, SDKs, webhooks and the developer portal', true),
    ('AUDITOR', 'Auditor', 'Read-only access to system activity, credential lifecycle and compliance logs', true);

-- Seed the initial resource/action permission catalogue scoped to E02 (FR-ORG-013). Later epics
-- add their own resources (DID, CREDENTIAL_SCHEMA, CREDENTIAL, VERIFICATION_POLICY, ...) in their
-- own migrations.
INSERT INTO permissions (resource, action, description) VALUES
    ('ORGANIZATION', 'CREATE', 'Create organizations'),
    ('ORGANIZATION', 'READ', 'View organizations'),
    ('ORGANIZATION', 'UPDATE', 'Update organization profile'),
    ('ORGANIZATION', 'DEACTIVATE', 'Deactivate an organization'),
    ('TENANT', 'CREATE', 'Create tenants'),
    ('TENANT', 'READ', 'View tenants'),
    ('TENANT', 'UPDATE', 'Update tenant configuration'),
    ('TENANT', 'SUSPEND', 'Suspend a tenant'),
    ('USER', 'INVITE', 'Invite users into a tenant'),
    ('USER', 'READ', 'View tenant users'),
    ('USER', 'SUSPEND', 'Suspend a user'),
    ('USER', 'DEACTIVATE', 'Deactivate a user'),
    ('ROLE', 'CREATE', 'Create custom roles'),
    ('ROLE', 'ASSIGN', 'Assign roles to users'),
    ('PERMISSION', 'MANAGE', 'Create permissions and attach them to roles');

-- PLATFORM_ADMIN gets every seeded permission; ORG_ADMIN gets everything except organization
-- creation/deactivation (platform-administered).
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p WHERE r.code = 'PLATFORM_ADMIN';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'ORG_ADMIN'
  AND NOT (p.resource = 'ORGANIZATION' AND p.action IN ('CREATE', 'DEACTIVATE'))
  AND NOT (p.resource = 'TENANT' AND p.action = 'CREATE');
