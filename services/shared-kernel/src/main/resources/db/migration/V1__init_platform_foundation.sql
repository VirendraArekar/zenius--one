CREATE TABLE platform_modules (
    id UUID PRIMARY KEY,
    code VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    tagline VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    category VARCHAR(64) NOT NULL,
    phase_number INTEGER NOT NULL,
    status VARCHAR(32) NOT NULL,
    route_path VARCHAR(128) NOT NULL,
    icon_name VARCHAR(64) NOT NULL,
    sort_order INTEGER NOT NULL,
    bounded_context VARCHAR(64) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system'
);

CREATE INDEX idx_platform_modules_category ON platform_modules (category);
CREATE INDEX idx_platform_modules_phase ON platform_modules (phase_number, sort_order);

CREATE TABLE system_settings (
    id UUID PRIMARY KEY,
    setting_key VARCHAR(120) NOT NULL UNIQUE,
    setting_value TEXT NOT NULL,
    category VARCHAR(64) NOT NULL,
    description VARCHAR(255) NOT NULL,
    is_public BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system'
);

CREATE INDEX idx_system_settings_category ON system_settings (category);

INSERT INTO platform_modules (id, code, name, tagline, description, category, phase_number, status, route_path, icon_name, sort_order, bounded_context) VALUES
('11111111-1111-1111-1111-111111110001', 'home', 'Home', 'Unified Command Center', 'Executive overview, system health, cross-app shortcuts, and quick launch.', 'PLATFORM', 1, 'ACTIVE', '/', 'LayoutDashboard', 1, 'platform'),
('11111111-1111-1111-1111-111111110002', 'crm', 'CRM', 'Customer Relationship Hub', 'Leads, contacts, accounts, pipelines, blueprints, and territory management.', 'REVENUE', 3, 'PLANNED', '/crm', 'Users', 2, 'crm'),
('11111111-1111-1111-1111-111111110003', 'sales', 'Sales', 'Quote-to-Cash Engine', 'Products, price books, quotes, sales orders, forecasting, and gamification.', 'REVENUE', 3, 'PLANNED', '/sales', 'TrendingUp', 3, 'crm'),
('11111111-1111-1111-1111-111111110004', 'marketing', 'Marketing', 'Omnichannel Growth Suite', 'Email campaigns, customer journeys, landing pages, social publishing, and attribution.', 'REVENUE', 4, 'PLANNED', '/marketing', 'Megaphone', 4, 'marketing'),
('11111111-1111-1111-1111-111111110005', 'support', 'Customer Support', 'Service Desk & Field Ops', 'Multi-channel tickets, SLA escalation, knowledge base, live chat, and field service.', 'CUSTOMER', 4, 'PLANNED', '/support', 'Headphones', 5, 'support'),
('11111111-1111-1111-1111-111111110006', 'finance', 'Finance', 'Accounting & Subscriptions', 'Chart of accounts, general ledger, AR/AP, India GST/TDS, billing, and dunning.', 'FINANCE', 5, 'PLANNED', '/finance', 'Landmark', 6, 'finance'),
('11111111-1111-1111-1111-111111110007', 'inventory', 'Inventory', 'Supply Chain & Commerce', 'Multi-warehouse stock, batch/serial tracking, purchase orders, and online store.', 'OPERATIONS', 5, 'PLANNED', '/inventory', 'Boxes', 7, 'inventory'),
('11111111-1111-1111-1111-111111110008', 'hr', 'HR', 'People & Attendance', 'Employee directory, org hierarchy, geo-attendance, leave workflows, and appraisals.', 'PEOPLE', 6, 'PLANNED', '/hr', 'UserCheck', 8, 'hr'),
('11111111-1111-1111-1111-111111110009', 'recruitment', 'Recruitment', 'Applicant Tracking System', 'Job openings, resume parsing, interview scheduling, scorecards, and offer letters.', 'PEOPLE', 6, 'PLANNED', '/recruitment', 'Briefcase', 9, 'recruitment'),
('11111111-1111-1111-1111-111111110010', 'payroll', 'Payroll', 'Compensation & Compliance', 'Salary structures, automated payroll runs, payslips, PF/ESI, and F&F settlement.', 'PEOPLE', 6, 'PLANNED', '/payroll', 'Wallet', 10, 'payroll'),
('11111111-1111-1111-1111-111111110011', 'projects', 'Projects', 'Agile & Resource Delivery', 'Kanban, Gantt, sprints, milestones, bug tracking, timesheets, and profitability.', 'OPERATIONS', 7, 'PLANNED', '/projects', 'FolderKanban', 11, 'projects'),
('11111111-1111-1111-1111-111111110012', 'collaboration', 'Collaboration', 'Team Chat & Meetings', 'Channels, direct messaging, voice/video calls, screen sharing, and calendars.', 'WORKPLACE', 7, 'PLANNED', '/collaboration', 'MessageSquare', 12, 'collaboration'),
('11111111-1111-1111-1111-111111110013', 'mail', 'Mail', 'Enterprise Business Email', 'Multi-mailbox client, shared inboxes, rules, signatures, and archiving.', 'WORKPLACE', 7, 'PLANNED', '/mail', 'Mail', 13, 'collaboration'),
('11111111-1111-1111-1111-111111110014', 'workdrive', 'WorkDrive', 'Cloud Document Vault', 'Shared folders, permissions, version history, retention, and legal contracts.', 'WORKPLACE', 7, 'PLANNED', '/workdrive', 'HardDrive', 14, 'documents'),
('11111111-1111-1111-1111-111111110015', 'office', 'Office Suite', 'Docs, Sheets, Slides & Sign', 'Real-time word processor, spreadsheets, presentations, PDF tools, and e-Sign.', 'WORKPLACE', 7, 'PLANNED', '/office', 'FileText', 15, 'documents'),
('11111111-1111-1111-1111-111111110016', 'analytics', 'Analytics', 'Enterprise BI & Forecasting', 'Drag-and-drop dashboards, pivot tables, cross-app reports, and exports.', 'INTELLIGENCE', 8, 'PLANNED', '/analytics', 'BarChart3', 16, 'analytics'),
('11111111-1111-1111-1111-111111110017', 'automation', 'Automation', 'Workflows & RPA Bots', 'Visual node designer, event triggers, retries, webhooks, and RPA schedules.', 'INTELLIGENCE', 8, 'PLANNED', '/automation', 'Workflow', 17, 'automation'),
('11111111-1111-1111-1111-111111110018', 'creator', 'Creator', 'Low-Code App Builder', 'Drag-and-drop forms, custom tables, formula fields, scripts, and app publishing.', 'INTELLIGENCE', 8, 'PLANNED', '/creator', 'Blocks', 18, 'creator'),
('11111111-1111-1111-1111-111111110019', 'ai', 'AI Assistant', 'Zenius AI Intelligence', 'Global assistant, 11 specialized agents, pgvector RAG, and natural-language workflows.', 'INTELLIGENCE', 9, 'PLANNED', '/ai', 'Sparkles', 19, 'ai'),
('11111111-1111-1111-1111-111111110020', 'admin', 'Administration', 'Identity, Orgs & Security', 'SSO, multi-org switching, RBAC, branches, audit logs, API keys, and IT assets.', 'PLATFORM', 2, 'PLANNED', '/admin', 'ShieldCheck', 20, 'identity');

INSERT INTO system_settings (id, setting_key, setting_value, category, description, is_public) VALUES
('22222222-2222-2222-2222-222222220001', 'platform.name', 'Zenius One', 'GENERAL', 'Product display name', TRUE),
('22222222-2222-2222-2222-222222220002', 'platform.architecture', 'Modular Monolith (Spring Boot 3.5 + React 19)', 'ARCHITECTURE', 'Core runtime architecture style', TRUE),
('22222222-2222-2222-2222-222222220003', 'platform.tenancy.mode', 'SHARED_DB_ROW_ISOLATED', 'SECURITY', 'Multi-tenant isolation model', TRUE),
('22222222-2222-2222-2222-222222220004', 'platform.current.phase', '1', 'ROADMAP', 'Current active implementation phase', TRUE);

