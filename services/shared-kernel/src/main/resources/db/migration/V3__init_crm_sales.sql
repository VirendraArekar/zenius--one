CREATE TABLE crm_accounts (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    parent_account_id UUID REFERENCES crm_accounts(id),
    owner_user_id UUID,
    name VARCHAR(180) NOT NULL,
    industry VARCHAR(100),
    website VARCHAR(200),
    phone VARCHAR(50),
    annual_revenue NUMERIC(18, 2),
    employee_count INTEGER,
    lifecycle_stage VARCHAR(40) NOT NULL DEFAULT 'PROSPECT',
    billing_city VARCHAR(100),
    billing_country VARCHAR(100),
    deleted_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system'
);
CREATE INDEX idx_crm_accounts_org ON crm_accounts (organization_id);

CREATE TABLE crm_contacts (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    account_id UUID REFERENCES crm_accounts(id),
    owner_user_id UUID,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(180),
    phone VARCHAR(50),
    job_title VARCHAR(120),
    department VARCHAR(100),
    segment VARCHAR(64),
    deleted_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system'
);
CREATE INDEX idx_crm_contacts_org ON crm_contacts (organization_id);
CREATE INDEX idx_crm_contacts_account ON crm_contacts (account_id);

CREATE TABLE crm_leads (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    owner_user_id UUID,
    full_name VARCHAR(160) NOT NULL,
    email VARCHAR(180),
    phone VARCHAR(50),
    company_name VARCHAR(180),
    job_title VARCHAR(120),
    source VARCHAR(64) NOT NULL DEFAULT 'WEBSITE',
    status VARCHAR(40) NOT NULL DEFAULT 'NEW',
    lead_score INTEGER NOT NULL DEFAULT 20,
    estimated_value NUMERIC(18, 2),
    notes TEXT,
    converted_account_id UUID REFERENCES crm_accounts(id),
    converted_contact_id UUID REFERENCES crm_contacts(id),
    converted_at TIMESTAMP WITH TIME ZONE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system'
);
CREATE INDEX idx_crm_leads_org_status ON crm_leads (organization_id, status);
CREATE INDEX idx_crm_leads_org_email ON crm_leads (organization_id, email);

CREATE TABLE crm_pipelines (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    name VARCHAR(120) NOT NULL,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system'
);
CREATE INDEX idx_crm_pipelines_org ON crm_pipelines (organization_id);

CREATE TABLE crm_stages (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    pipeline_id UUID NOT NULL REFERENCES crm_pipelines(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    probability INTEGER NOT NULL DEFAULT 20,
    stage_order INTEGER NOT NULL,
    is_closed BOOLEAN NOT NULL DEFAULT FALSE,
    is_won BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system'
);
CREATE INDEX idx_crm_stages_pipeline ON crm_stages (pipeline_id, stage_order);

CREATE TABLE crm_deals (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    pipeline_id UUID NOT NULL REFERENCES crm_pipelines(id),
    stage_id UUID NOT NULL REFERENCES crm_stages(id),
    account_id UUID REFERENCES crm_accounts(id),
    contact_id UUID REFERENCES crm_contacts(id),
    owner_user_id UUID,
    title VARCHAR(200) NOT NULL,
    amount NUMERIC(18, 2) NOT NULL DEFAULT 0,
    probability INTEGER NOT NULL DEFAULT 20,
    expected_revenue NUMERIC(18, 2) NOT NULL DEFAULT 0,
    expected_close_date DATE,
    lost_reason VARCHAR(255),
    deleted_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system'
);
CREATE INDEX idx_crm_deals_org_stage ON crm_deals (organization_id, stage_id);

CREATE TABLE crm_products (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    sku VARCHAR(64) NOT NULL,
    name VARCHAR(180) NOT NULL,
    description VARCHAR(500),
    category VARCHAR(80),
    unit_price NUMERIC(18, 2) NOT NULL,
    tax_rate NUMERIC(5, 2) NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system',
    CONSTRAINT uq_crm_products_org_sku UNIQUE (organization_id, sku)
);
CREATE INDEX idx_crm_products_org ON crm_products (organization_id);

CREATE TABLE crm_quotes (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    quote_number VARCHAR(64) NOT NULL,
    account_id UUID REFERENCES crm_accounts(id),
    contact_id UUID REFERENCES crm_contacts(id),
    deal_id UUID REFERENCES crm_deals(id),
    owner_user_id UUID,
    subject VARCHAR(200) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    valid_until DATE,
    subtotal NUMERIC(18, 2) NOT NULL DEFAULT 0,
    tax_total NUMERIC(18, 2) NOT NULL DEFAULT 0,
    grand_total NUMERIC(18, 2) NOT NULL DEFAULT 0,
    notes TEXT,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system',
    CONSTRAINT uq_crm_quotes_org_num UNIQUE (organization_id, quote_number)
);
CREATE INDEX idx_crm_quotes_org ON crm_quotes (organization_id);

CREATE TABLE crm_quote_lines (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    quote_id UUID NOT NULL REFERENCES crm_quotes(id) ON DELETE CASCADE,
    product_id UUID REFERENCES crm_products(id),
    item_name VARCHAR(180) NOT NULL,
    quantity NUMERIC(12, 2) NOT NULL,
    unit_price NUMERIC(18, 2) NOT NULL,
    discount_percent NUMERIC(5, 2) NOT NULL DEFAULT 0,
    tax_rate NUMERIC(5, 2) NOT NULL DEFAULT 0,
    line_total NUMERIC(18, 2) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system'
);

CREATE TABLE crm_sales_orders (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    order_number VARCHAR(64) NOT NULL,
    quote_id UUID REFERENCES crm_quotes(id),
    account_id UUID REFERENCES crm_accounts(id),
    deal_id UUID REFERENCES crm_deals(id),
    subject VARCHAR(200) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'CONFIRMED',
    order_date DATE NOT NULL,
    grand_total NUMERIC(18, 2) NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system',
    CONSTRAINT uq_crm_orders_org_num UNIQUE (organization_id, order_number)
);
CREATE INDEX idx_crm_orders_org ON crm_sales_orders (organization_id);

CREATE TABLE crm_activities (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    owner_user_id UUID,
    activity_type VARCHAR(32) NOT NULL,
    subject VARCHAR(200) NOT NULL,
    description TEXT,
    related_entity_type VARCHAR(32) NOT NULL,
    related_entity_id UUID NOT NULL,
    due_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system'
);
CREATE INDEX idx_crm_activities_related ON crm_activities (organization_id, related_entity_type, related_entity_id);

INSERT INTO permissions (id, module_code, permission_key, description) VALUES
('44444444-4444-4444-4444-444444440001', 'crm', 'crm:lead:read', 'View CRM leads'),
('44444444-4444-4444-4444-444444440002', 'crm', 'crm:lead:write', 'Create, update, import, and archive CRM leads'),
('44444444-4444-4444-4444-444444440003', 'crm', 'crm:lead:convert', 'Convert qualified leads into accounts, contacts, and deals'),
('44444444-4444-4444-4444-444444440004', 'crm', 'crm:contact:read', 'View CRM contacts'),
('44444444-4444-4444-4444-444444440005', 'crm', 'crm:contact:write', 'Create and manage CRM contacts'),
('44444444-4444-4444-4444-444444440006', 'crm', 'crm:account:read', 'View CRM company accounts'),
('44444444-4444-4444-4444-444444440007', 'crm', 'crm:account:write', 'Create and manage CRM company accounts'),
('44444444-4444-4444-4444-444444440008', 'crm', 'crm:deal:read', 'View sales pipelines and deals'),
('44444444-4444-4444-4444-444444440009', 'crm', 'crm:deal:write', 'Create, update, and advance pipeline deals'),
('44444444-4444-4444-4444-444444440010', 'crm', 'crm:product:read', 'View sales products and services catalog'),
('44444444-4444-4444-4444-444444440011', 'crm', 'crm:product:write', 'Create and manage sales products and services'),
('44444444-4444-4444-4444-444444440012', 'crm', 'crm:quote:read', 'View quotes and sales orders'),
('44444444-4444-4444-4444-444444440013', 'crm', 'crm:quote:write', 'Create quotes and convert quotes to sales orders'),
('44444444-4444-4444-4444-444444440014', 'crm', 'crm:activity:read', 'View CRM activities and tasks'),
('44444444-4444-4444-4444-444444440015', 'crm', 'crm:activity:write', 'Log CRM calls, meetings, and tasks');

-- Grant new CRM permissions to all existing ORG_OWNER and ORG_ADMIN roles
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.code IN ('ORG_OWNER', 'ORG_ADMIN')
  AND p.module_code = 'crm'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

UPDATE platform_modules SET status = 'ACTIVE' WHERE code IN ('crm', 'sales');
UPDATE system_settings SET setting_value = '3' WHERE setting_key = 'platform.current.phase';

