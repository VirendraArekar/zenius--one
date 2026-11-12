-- Project Management
CREATE TABLE proj_projects (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    project_code VARCHAR(40) NOT NULL,
    name VARCHAR(160) NOT NULL,
    client_name VARCHAR(160),
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    budget_amount NUMERIC(18, 2) NOT NULL DEFAULT 0,
    start_date DATE,
    due_date DATE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system',
    CONSTRAINT uq_proj_org_code UNIQUE (organization_id, project_code)
);

CREATE TABLE proj_sprints (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    project_id UUID NOT NULL REFERENCES proj_projects(id) ON DELETE CASCADE,
    name VARCHAR(140) NOT NULL,
    goal VARCHAR(300),
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system'
);

CREATE TABLE proj_tasks (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    project_id UUID NOT NULL REFERENCES proj_projects(id) ON DELETE CASCADE,
    sprint_id UUID REFERENCES proj_sprints(id),
    task_key VARCHAR(48) NOT NULL,
    title VARCHAR(200) NOT NULL,
    task_type VARCHAR(24) NOT NULL DEFAULT 'TASK',
    priority VARCHAR(24) NOT NULL DEFAULT 'MEDIUM',
    status VARCHAR(32) NOT NULL DEFAULT 'TODO',
    story_points INTEGER NOT NULL DEFAULT 1,
    assignee_name VARCHAR(150),
    due_date DATE,
    logged_minutes INTEGER NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system'
);

CREATE TABLE proj_time_logs (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    project_id UUID NOT NULL REFERENCES proj_projects(id) ON DELETE CASCADE,
    task_id UUID NOT NULL REFERENCES proj_tasks(id) ON DELETE CASCADE,
    member_name VARCHAR(150) NOT NULL,
    minutes_logged INTEGER NOT NULL,
    billable BOOLEAN NOT NULL DEFAULT TRUE,
    work_date DATE NOT NULL,
    notes VARCHAR(300),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system'
);

-- Mail & Collaboration
CREATE TABLE col_mail_messages (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    folder VARCHAR(32) NOT NULL DEFAULT 'INBOX',
    sender_email VARCHAR(180) NOT NULL,
    recipient_email VARCHAR(180) NOT NULL,
    subject VARCHAR(200) NOT NULL,
    body TEXT NOT NULL,
    labels VARCHAR(160),
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    starred BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system'
);

CREATE TABLE col_channels (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    is_private BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system',
    CONSTRAINT uq_col_channels_org_name UNIQUE (organization_id, name)
);

CREATE TABLE col_messages (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    channel_id UUID NOT NULL REFERENCES col_channels(id) ON DELETE CASCADE,
    sender_name VARCHAR(150) NOT NULL,
    body TEXT NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system'
);

CREATE TABLE col_meetings (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    title VARCHAR(180) NOT NULL,
    organizer_name VARCHAR(150) NOT NULL,
    scheduled_at TIMESTAMP WITH TIME ZONE NOT NULL,
    duration_minutes INTEGER NOT NULL DEFAULT 30,
    meeting_link VARCHAR(255) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system'
);

-- WorkDrive, Office Suite & Digital Signatures
CREATE TABLE doc_folders (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    parent_folder_id UUID REFERENCES doc_folders(id),
    name VARCHAR(140) NOT NULL,
    shared BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system'
);

CREATE TABLE doc_files (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    folder_id UUID REFERENCES doc_folders(id),
    title VARCHAR(180) NOT NULL,
    doc_type VARCHAR(32) NOT NULL DEFAULT 'WORD',
    content_body TEXT NOT NULL,
    current_version INTEGER NOT NULL DEFAULT 1,
    retention_policy VARCHAR(64) DEFAULT 'STANDARD_7Y',
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system'
);

CREATE TABLE doc_revisions (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    document_id UUID NOT NULL REFERENCES doc_files(id) ON DELETE CASCADE,
    revision_number INTEGER NOT NULL,
    editor_name VARCHAR(150) NOT NULL,
    content_snapshot TEXT NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system'
);

CREATE TABLE doc_signature_requests (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    document_id UUID NOT NULL REFERENCES doc_files(id) ON DELETE CASCADE,
    signer_name VARCHAR(150) NOT NULL,
    signer_email VARCHAR(180) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    signature_hash VARCHAR(128),
    signed_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system'
);

-- Legal & Contract Management
CREATE TABLE leg_contracts (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    contract_number VARCHAR(40) NOT NULL,
    title VARCHAR(180) NOT NULL,
    counterparty_name VARCHAR(160) NOT NULL,
    contract_type VARCHAR(48) NOT NULL,
    value_amount NUMERIC(18, 2) NOT NULL DEFAULT 0,
    effective_date DATE NOT NULL,
    expiration_date DATE NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    document_id UUID REFERENCES doc_files(id),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system',
    CONSTRAINT uq_leg_contracts_org_num UNIQUE (organization_id, contract_number)
);

INSERT INTO permissions (id, module_code, permission_key, description) VALUES
('88888888-8888-8888-8888-888888880001', 'projects', 'projects:read', 'View projects, sprints, tasks, and timesheets'),
('88888888-8888-8888-8888-888888880002', 'projects', 'projects:write', 'Create projects, tasks, sprints, and log time'),
('88888888-8888-8888-8888-888888880003', 'collaboration', 'collab:read', 'Read business mail, team channels, and meetings'),
('88888888-8888-8888-8888-888888880004', 'collaboration', 'collab:write', 'Send business mail, post channel messages, and schedule meetings'),
('88888888-8888-8888-8888-888888880005', 'documents', 'docs:read', 'View WorkDrive folders, Office documents, and signature requests'),
('88888888-8888-8888-8888-888888880006', 'documents', 'docs:write', 'Create/edit Office documents, revisions, and request/sign e-Signatures'),
('88888888-8888-8888-8888-888888880007', 'legal', 'legal:read', 'View legal contracts and compliance lifecycle'),
('88888888-8888-8888-8888-888888880008', 'legal', 'legal:write', 'Create, approve, and execute legal contracts');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.code IN ('ORG_OWNER', 'ORG_ADMIN')
  AND p.module_code IN ('projects', 'collaboration', 'documents', 'legal')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

UPDATE platform_modules SET status = 'ACTIVE' WHERE code IN ('projects', 'collaboration', 'mail', 'workdrive', 'office');
UPDATE system_settings SET setting_value = '7' WHERE setting_key = 'platform.current.phase';

