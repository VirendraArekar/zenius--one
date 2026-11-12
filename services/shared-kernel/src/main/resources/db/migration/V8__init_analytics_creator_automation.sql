-- Phase 8: Analytics & BI, Low-Code Creator, Workflow Automation & RPA

-- BI Dashboards & Reports
CREATE TABLE bi_dashboards (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    name VARCHAR(160) NOT NULL,
    description VARCHAR(500),
    shared BOOLEAN NOT NULL DEFAULT TRUE,
    layout_json TEXT NOT NULL DEFAULT '[]',
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system'
);

CREATE TABLE bi_reports (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    dashboard_id UUID REFERENCES bi_dashboards(id) ON DELETE SET NULL,
    name VARCHAR(160) NOT NULL,
    data_source VARCHAR(48) NOT NULL,
    chart_type VARCHAR(32) NOT NULL DEFAULT 'BAR',
    metric VARCHAR(64) NOT NULL DEFAULT 'COUNT',
    dimension VARCHAR(64) NOT NULL DEFAULT 'STATUS',
    config_json TEXT NOT NULL DEFAULT '{}',
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system'
);

-- Low-Code Creator Platform
CREATE TABLE crt_apps (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    app_code VARCHAR(40) NOT NULL,
    name VARCHAR(160) NOT NULL,
    description VARCHAR(500),
    status VARCHAR(24) NOT NULL DEFAULT 'DRAFT',
    app_version INTEGER NOT NULL DEFAULT 1,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system',
    CONSTRAINT uq_crt_apps_org_code UNIQUE (organization_id, app_code)
);

CREATE TABLE crt_forms (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    app_id UUID NOT NULL REFERENCES crt_apps(id) ON DELETE CASCADE,
    form_code VARCHAR(40) NOT NULL,
    name VARCHAR(160) NOT NULL,
    schema_json TEXT NOT NULL DEFAULT '[]',
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system',
    CONSTRAINT uq_crt_forms_org_code UNIQUE (organization_id, form_code)
);

CREATE TABLE crt_records (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    form_id UUID NOT NULL REFERENCES crt_forms(id) ON DELETE CASCADE,
    record_number VARCHAR(40) NOT NULL,
    data_json TEXT NOT NULL DEFAULT '{}',
    submitted_by VARCHAR(160) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system'
);

-- Workflow Automation & RPA
CREATE TABLE aut_workflows (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    name VARCHAR(160) NOT NULL,
    trigger_event VARCHAR(64) NOT NULL,
    condition_expr VARCHAR(255),
    action_type VARCHAR(64) NOT NULL,
    action_config_json TEXT NOT NULL DEFAULT '{}',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    run_count INTEGER NOT NULL DEFAULT 0,
    last_run_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system'
);

CREATE TABLE aut_executions (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    workflow_id UUID NOT NULL REFERENCES aut_workflows(id) ON DELETE CASCADE,
    trigger_payload_json TEXT NOT NULL DEFAULT '{}',
    status VARCHAR(24) NOT NULL,
    output_summary VARCHAR(500) NOT NULL,
    duration_ms BIGINT NOT NULL DEFAULT 0,
    executed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system'
);

-- RBAC Permissions for Phase 8
INSERT INTO permissions (id, module_code, permission_key, description) VALUES
('99999999-9999-9999-9999-999999990001', 'analytics', 'analytics:read', 'View BI dashboards, cross-app metrics, and export reports'),
('99999999-9999-9999-9999-999999990002', 'analytics', 'analytics:write', 'Create and edit BI dashboards and custom reports'),
('99999999-9999-9999-9999-999999990003', 'creator', 'creator:read', 'View low-code apps, forms, and custom table records'),
('99999999-9999-9999-9999-999999990004', 'creator', 'creator:write', 'Build, publish, and submit records to low-code apps'),
('99999999-9999-9999-9999-999999990005', 'automation', 'automation:read', 'View workflow automation rules, RPA bots, and execution logs'),
('99999999-9999-9999-9999-999999990006', 'automation', 'automation:write', 'Create, toggle, and trigger workflows and RPA automations');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.code IN ('ORG_OWNER', 'ORG_ADMIN')
  AND p.module_code IN ('analytics', 'creator', 'automation')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

UPDATE platform_modules SET status = 'ACTIVE' WHERE code IN ('analytics', 'automation', 'creator');
UPDATE system_settings SET setting_value = '8' WHERE setting_key = 'platform.current.phase';

