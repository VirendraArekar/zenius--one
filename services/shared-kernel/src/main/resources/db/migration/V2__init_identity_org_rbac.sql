CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(180) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(150) NOT NULL,
    job_title VARCHAR(120),
    phone VARCHAR(40),
    mfa_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    mfa_secret VARCHAR(128),
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    last_login_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system'
);

CREATE TABLE organizations (
    id UUID PRIMARY KEY,
    slug VARCHAR(80) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    industry VARCHAR(100),
    base_currency VARCHAR(10) NOT NULL DEFAULT 'USD',
    timezone VARCHAR(64) NOT NULL DEFAULT 'UTC',
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system'
);

CREATE TABLE branches (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    code VARCHAR(40) NOT NULL,
    name VARCHAR(120) NOT NULL,
    city VARCHAR(80),
    country VARCHAR(80),
    address VARCHAR(255),
    is_headquarters BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    deleted_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system',
    CONSTRAINT uq_branches_org_code UNIQUE (organization_id, code)
);
CREATE INDEX idx_branches_org ON branches (organization_id);

CREATE TABLE departments (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    code VARCHAR(40) NOT NULL,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(255),
    parent_department_id UUID REFERENCES departments(id),
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    deleted_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system',
    CONSTRAINT uq_departments_org_code UNIQUE (organization_id, code)
);
CREATE INDEX idx_departments_org ON departments (organization_id);

CREATE TABLE org_memberships (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    user_id UUID NOT NULL REFERENCES users(id),
    branch_id UUID REFERENCES branches(id),
    department_id UUID REFERENCES departments(id),
    employee_code VARCHAR(50),
    designation VARCHAR(120),
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    joined_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system',
    CONSTRAINT uq_org_memberships_org_user UNIQUE (organization_id, user_id)
);
CREATE INDEX idx_org_memberships_user ON org_memberships (user_id);
CREATE INDEX idx_org_memberships_org ON org_memberships (organization_id);

CREATE TABLE permissions (
    id UUID PRIMARY KEY,
    module_code VARCHAR(64) NOT NULL,
    permission_key VARCHAR(120) NOT NULL UNIQUE,
    description VARCHAR(255) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system'
);

CREATE TABLE roles (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    code VARCHAR(64) NOT NULL,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(255),
    is_system BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system',
    CONSTRAINT uq_roles_org_code UNIQUE (organization_id, code)
);
CREATE INDEX idx_roles_org ON roles (organization_id);

CREATE TABLE role_permissions (
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE member_roles (
    membership_id UUID NOT NULL REFERENCES org_memberships(id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (membership_id, role_id)
);

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    organization_id UUID REFERENCES organizations(id),
    family_id UUID NOT NULL,
    token_hash VARCHAR(128) NOT NULL UNIQUE,
    user_agent VARCHAR(255),
    ip_address VARCHAR(64),
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at TIMESTAMP WITH TIME ZONE,
    replaced_by_token_id UUID,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system'
);
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_family ON refresh_tokens (family_id);

CREATE TABLE api_keys (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    created_by_user_id UUID NOT NULL REFERENCES users(id),
    name VARCHAR(120) NOT NULL,
    key_prefix VARCHAR(16) NOT NULL,
    key_hash VARCHAR(128) NOT NULL UNIQUE,
    scopes VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE,
    last_used_at TIMESTAMP WITH TIME ZONE,
    revoked_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system'
);
CREATE INDEX idx_api_keys_org ON api_keys (organization_id);

CREATE TABLE audit_logs (
    id UUID PRIMARY KEY,
    organization_id UUID REFERENCES organizations(id),
    actor_user_id UUID,
    actor_email VARCHAR(180),
    action VARCHAR(100) NOT NULL,
    resource_type VARCHAR(80) NOT NULL,
    resource_id VARCHAR(100),
    summary VARCHAR(500) NOT NULL,
    ip_address VARCHAR(64),
    status VARCHAR(32) NOT NULL DEFAULT 'SUCCESS',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_audit_logs_org_created ON audit_logs (organization_id, created_at DESC);

CREATE TABLE notifications (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    recipient_user_id UUID NOT NULL REFERENCES users(id),
    title VARCHAR(160) NOT NULL,
    message VARCHAR(500) NOT NULL,
    category VARCHAR(48) NOT NULL DEFAULT 'SYSTEM',
    link_path VARCHAR(200),
    read_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system'
);
CREATE INDEX idx_notifications_recipient ON notifications (organization_id, recipient_user_id, read_at);

INSERT INTO permissions (id, module_code, permission_key, description) VALUES
('33333333-3333-3333-3333-333333330001', 'organization', 'org:read', 'View organization settings and profile'),
('33333333-3333-3333-3333-333333330002', 'organization', 'org:write', 'Update organization settings and profile'),
('33333333-3333-3333-3333-333333330003', 'organization', 'branch:read', 'View organization branches and locations'),
('33333333-3333-3333-3333-333333330004', 'organization', 'branch:write', 'Create, edit, or archive branches'),
('33333333-3333-3333-3333-333333330005', 'organization', 'department:read', 'View departments'),
('33333333-3333-3333-3333-333333330006', 'organization', 'department:write', 'Create, edit, or archive departments'),
('33333333-3333-3333-3333-333333330007', 'organization', 'member:read', 'View organization employees and members'),
('33333333-3333-3333-3333-333333330008', 'organization', 'member:write', 'Invite, update, and assign roles to members'),
('33333333-3333-3333-3333-333333330009', 'organization', 'role:read', 'View RBAC roles and permission matrices'),
('33333333-3333-3333-3333-333333330010', 'organization', 'role:write', 'Create and update custom RBAC roles'),
('33333333-3333-3333-3333-333333330011', 'itsecurity', 'audit:read', 'View security and organization audit logs'),
('33333333-3333-3333-3333-333333330012', 'integration', 'apikey:read', 'View organization API keys'),
('33333333-3333-3333-3333-333333330013', 'integration', 'apikey:write', 'Create and revoke organization API keys'),
('33333333-3333-3333-3333-333333330014', 'platform', 'notification:read', 'Read personal organization notifications');

UPDATE platform_modules SET status = 'ACTIVE' WHERE code = 'admin';
UPDATE system_settings SET setting_value = '2' WHERE setting_key = 'platform.current.phase';

