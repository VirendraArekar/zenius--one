CREATE TABLE mkt_subscribers (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    email VARCHAR(180) NOT NULL,
    full_name VARCHAR(160),
    source VARCHAR(64) NOT NULL DEFAULT 'MANUAL',
    tags VARCHAR(255),
    status VARCHAR(32) NOT NULL DEFAULT 'SUBSCRIBED',
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system',
    CONSTRAINT uq_mkt_subscribers_org_email UNIQUE (organization_id, email)
);
CREATE INDEX idx_mkt_subscribers_org ON mkt_subscribers (organization_id, status);

CREATE TABLE mkt_segments (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    name VARCHAR(120) NOT NULL,
    description VARCHAR(255),
    filter_tag VARCHAR(64),
    filter_source VARCHAR(64),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system'
);

CREATE TABLE mkt_campaigns (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    segment_id UUID REFERENCES mkt_segments(id),
    name VARCHAR(160) NOT NULL,
    channel VARCHAR(32) NOT NULL DEFAULT 'EMAIL',
    subject VARCHAR(200) NOT NULL,
    body_content TEXT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    scheduled_at TIMESTAMP WITH TIME ZONE,
    sent_at TIMESTAMP WITH TIME ZONE,
    sent_count INTEGER NOT NULL DEFAULT 0,
    opened_count INTEGER NOT NULL DEFAULT 0,
    clicked_count INTEGER NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system'
);
CREATE INDEX idx_mkt_campaigns_org ON mkt_campaigns (organization_id, status);

CREATE TABLE mkt_journeys (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    name VARCHAR(150) NOT NULL,
    trigger_event VARCHAR(64) NOT NULL,
    steps_summary VARCHAR(500) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    enrolled_count INTEGER NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system'
);

CREATE TABLE mkt_forms (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    name VARCHAR(150) NOT NULL,
    title VARCHAR(180) NOT NULL,
    submit_label VARCHAR(64) NOT NULL DEFAULT 'Submit',
    auto_create_lead BOOLEAN NOT NULL DEFAULT TRUE,
    submissions_count INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system'
);

CREATE TABLE mkt_coupons (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    code VARCHAR(48) NOT NULL,
    discount_percent NUMERIC(5, 2) NOT NULL,
    max_redemptions INTEGER,
    redemption_count INTEGER NOT NULL DEFAULT 0,
    valid_until DATE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system',
    CONSTRAINT uq_mkt_coupons_org_code UNIQUE (organization_id, code)
);

-- Customer Support & Service Tables

CREATE TABLE sup_sla_policies (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    name VARCHAR(120) NOT NULL,
    priority VARCHAR(32) NOT NULL,
    first_response_minutes INTEGER NOT NULL,
    resolution_minutes INTEGER NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system',
    CONSTRAINT uq_sup_sla_org_priority UNIQUE (organization_id, priority)
);

CREATE TABLE sup_tickets (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    ticket_number VARCHAR(40) NOT NULL,
    subject VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    channel VARCHAR(32) NOT NULL DEFAULT 'WEB',
    priority VARCHAR(32) NOT NULL DEFAULT 'MEDIUM',
    status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
    requester_name VARCHAR(150) NOT NULL,
    requester_email VARCHAR(180) NOT NULL,
    assignee_user_id UUID,
    sla_due_at TIMESTAMP WITH TIME ZONE,
    first_responded_at TIMESTAMP WITH TIME ZONE,
    resolved_at TIMESTAMP WITH TIME ZONE,
    escalated BOOLEAN NOT NULL DEFAULT FALSE,
    csat_rating INTEGER,
    csat_feedback VARCHAR(500),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system',
    CONSTRAINT uq_sup_tickets_org_num UNIQUE (organization_id, ticket_number)
);
CREATE INDEX idx_sup_tickets_org_status ON sup_tickets (organization_id, status, priority);

CREATE TABLE sup_ticket_comments (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    ticket_id UUID NOT NULL REFERENCES sup_tickets(id) ON DELETE CASCADE,
    author_user_id UUID,
    author_name VARCHAR(150) NOT NULL,
    body TEXT NOT NULL,
    internal_note BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system'
);

CREATE TABLE sup_canned_responses (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    title VARCHAR(150) NOT NULL,
    category VARCHAR(64),
    body TEXT NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system'
);

CREATE TABLE sup_kb_articles (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    title VARCHAR(200) NOT NULL,
    category VARCHAR(80) NOT NULL,
    content TEXT NOT NULL,
    published BOOLEAN NOT NULL DEFAULT TRUE,
    helpful_count INTEGER NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system'
);

CREATE TABLE sup_service_appointments (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    ticket_id UUID REFERENCES sup_tickets(id),
    appointment_number VARCHAR(40) NOT NULL,
    customer_name VARCHAR(150) NOT NULL,
    service_address VARCHAR(255) NOT NULL,
    technician_user_id UUID,
    scheduled_start TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'SCHEDULED',
    notes VARCHAR(500),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system',
    CONSTRAINT uq_sup_appointments_org_num UNIQUE (organization_id, appointment_number)
);

INSERT INTO permissions (id, module_code, permission_key, description) VALUES
('55555555-5555-5555-5555-555555550001', 'marketing', 'marketing:read', 'View campaigns, subscribers, forms, and marketing analytics'),
('55555555-5555-5555-5555-555555550002', 'marketing', 'marketing:write', 'Create and launch campaigns, journeys, segments, forms, and coupons'),
('55555555-5555-5555-5555-555555550003', 'support', 'support:ticket:read', 'View support tickets, comments, and SLA dashboards'),
('55555555-5555-5555-5555-555555550004', 'support', 'support:ticket:write', 'Create, reply, escalate, and resolve support tickets'),
('55555555-5555-5555-5555-555555550005', 'support', 'support:kb:read', 'Read knowledge base articles and canned responses'),
('55555555-5555-5555-5555-555555550006', 'support', 'support:kb:write', 'Author and publish knowledge base articles and canned responses'),
('55555555-5555-5555-5555-555555550007', 'support', 'support:field:read', 'View field service appointments'),
('55555555-5555-5555-5555-555555550008', 'support', 'support:field:write', 'Schedule and update field service appointments');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.code IN ('ORG_OWNER', 'ORG_ADMIN')
  AND p.module_code IN ('marketing', 'support')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

UPDATE platform_modules SET status = 'ACTIVE' WHERE code IN ('marketing', 'support');
UPDATE system_settings SET setting_value = '4' WHERE setting_key = 'platform.current.phase';

