-- Phase 10: Enterprise Security Hardening, IT Asset Inventory & Incident Response

CREATE TABLE sec_it_assets (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    asset_tag VARCHAR(40) NOT NULL,
    name VARCHAR(160) NOT NULL,
    asset_type VARCHAR(40) NOT NULL,
    owner_name VARCHAR(150),
    os_version VARCHAR(100),
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    compliance_state VARCHAR(32) NOT NULL DEFAULT 'COMPLIANT',
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system',
    CONSTRAINT uq_sec_it_assets_org_tag UNIQUE (organization_id, asset_tag)
);

CREATE TABLE sec_incidents (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    incident_number VARCHAR(40) NOT NULL,
    title VARCHAR(180) NOT NULL,
    severity VARCHAR(24) NOT NULL DEFAULT 'MEDIUM',
    status VARCHAR(24) NOT NULL DEFAULT 'OPEN',
    reporter_email VARCHAR(180) NOT NULL,
    resolution_notes VARCHAR(600),
    resolved_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system',
    CONSTRAINT uq_sec_incidents_org_num UNIQUE (organization_id, incident_number)
);

CREATE TABLE sec_policies (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    min_password_length INTEGER NOT NULL DEFAULT 12,
    require_mfa BOOLEAN NOT NULL DEFAULT FALSE,
    session_timeout_minutes INTEGER NOT NULL DEFAULT 480,
    allowed_ip_cidr VARCHAR(120) DEFAULT '0.0.0.0/0',
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system',
    CONSTRAINT uq_sec_policies_org UNIQUE (organization_id)
);

INSERT INTO permissions (id, module_code, permission_key, description) VALUES
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbb0001', 'itsecurity', 'itsec:read', 'View IT asset inventory, security incidents, and compliance policies'),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbb0002', 'itsecurity', 'itsec:write', 'Register IT assets, resolve security incidents, and update security policies');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.code IN ('ORG_OWNER', 'ORG_ADMIN')
  AND p.module_code = 'itsecurity'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

UPDATE system_settings SET setting_value = '10' WHERE setting_key = 'platform.current.phase';

