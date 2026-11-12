-- Finance & Accounting
CREATE TABLE fin_accounts (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    code VARCHAR(32) NOT NULL,
    name VARCHAR(140) NOT NULL,
    account_type VARCHAR(32) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'USD',
    is_system BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system',
    CONSTRAINT uq_fin_accounts_org_code UNIQUE (organization_id, code)
);

CREATE TABLE fin_journal_entries (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    entry_number VARCHAR(40) NOT NULL,
    entry_date DATE NOT NULL,
    reference VARCHAR(120),
    description VARCHAR(300) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'POSTED',
    total_debit NUMERIC(18, 2) NOT NULL,
    total_credit NUMERIC(18, 2) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system',
    CONSTRAINT uq_fin_journals_org_num UNIQUE (organization_id, entry_number)
);

CREATE TABLE fin_journal_lines (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    journal_entry_id UUID NOT NULL REFERENCES fin_journal_entries(id) ON DELETE CASCADE,
    account_id UUID NOT NULL REFERENCES fin_accounts(id),
    description VARCHAR(200),
    debit_amount NUMERIC(18, 2) NOT NULL DEFAULT 0,
    credit_amount NUMERIC(18, 2) NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system'
);
CREATE INDEX idx_fin_journal_lines_account ON fin_journal_lines (organization_id, account_id);

CREATE TABLE fin_invoices (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    invoice_number VARCHAR(40) NOT NULL,
    customer_name VARCHAR(160) NOT NULL,
    customer_email VARCHAR(180),
    currency VARCHAR(10) NOT NULL DEFAULT 'USD',
    issue_date DATE NOT NULL,
    due_date DATE NOT NULL,
    subtotal NUMERIC(18, 2) NOT NULL,
    gst_rate NUMERIC(5, 2) NOT NULL DEFAULT 0,
    gst_amount NUMERIC(18, 2) NOT NULL DEFAULT 0,
    tds_rate NUMERIC(5, 2) NOT NULL DEFAULT 0,
    tds_amount NUMERIC(18, 2) NOT NULL DEFAULT 0,
    total_amount NUMERIC(18, 2) NOT NULL,
    balance_due NUMERIC(18, 2) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'SENT',
    notes VARCHAR(400),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system',
    CONSTRAINT uq_fin_invoices_org_num UNIQUE (organization_id, invoice_number)
);

CREATE TABLE fin_bills (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    bill_number VARCHAR(40) NOT NULL,
    vendor_name VARCHAR(160) NOT NULL,
    bill_date DATE NOT NULL,
    due_date DATE NOT NULL,
    subtotal NUMERIC(18, 2) NOT NULL,
    tds_rate NUMERIC(5, 2) NOT NULL DEFAULT 0,
    tds_amount NUMERIC(18, 2) NOT NULL DEFAULT 0,
    total_amount NUMERIC(18, 2) NOT NULL,
    balance_due NUMERIC(18, 2) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system',
    CONSTRAINT uq_fin_bills_org_num UNIQUE (organization_id, bill_number)
);

CREATE TABLE fin_payments (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    payment_type VARCHAR(24) NOT NULL,
    reference_id UUID NOT NULL,
    payment_date DATE NOT NULL,
    amount NUMERIC(18, 2) NOT NULL,
    method VARCHAR(48) NOT NULL DEFAULT 'BANK_TRANSFER',
    reference_note VARCHAR(200),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system'
);

-- Billing & Subscriptions
CREATE TABLE bill_plans (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    code VARCHAR(64) NOT NULL,
    name VARCHAR(140) NOT NULL,
    billing_interval VARCHAR(24) NOT NULL DEFAULT 'MONTHLY',
    price NUMERIC(18, 2) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'USD',
    trial_days INTEGER NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system',
    CONSTRAINT uq_bill_plans_org_code UNIQUE (organization_id, code)
);

CREATE TABLE bill_subscriptions (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    subscription_number VARCHAR(40) NOT NULL,
    plan_id UUID NOT NULL REFERENCES bill_plans(id),
    customer_name VARCHAR(160) NOT NULL,
    customer_email VARCHAR(180) NOT NULL,
    status VARCHAR(32) NOT NULL,
    current_period_start DATE NOT NULL,
    current_period_end DATE NOT NULL,
    credit_balance NUMERIC(18, 2) NOT NULL DEFAULT 0,
    dunning_attempts INTEGER NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system',
    CONSTRAINT uq_bill_subs_org_num UNIQUE (organization_id, subscription_number)
);

-- Inventory & Commerce
CREATE TABLE inv_warehouses (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    code VARCHAR(40) NOT NULL,
    name VARCHAR(140) NOT NULL,
    city VARCHAR(80),
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system',
    CONSTRAINT uq_inv_warehouses_org_code UNIQUE (organization_id, code)
);

CREATE TABLE inv_items (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    sku VARCHAR(64) NOT NULL,
    name VARCHAR(180) NOT NULL,
    category VARCHAR(80),
    unit_cost NUMERIC(18, 2) NOT NULL DEFAULT 0,
    selling_price NUMERIC(18, 2) NOT NULL DEFAULT 0,
    reorder_level INTEGER NOT NULL DEFAULT 10,
    track_serial BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system',
    CONSTRAINT uq_inv_items_org_sku UNIQUE (organization_id, sku)
);

CREATE TABLE inv_stock_levels (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    item_id UUID NOT NULL REFERENCES inv_items(id) ON DELETE CASCADE,
    warehouse_id UUID NOT NULL REFERENCES inv_warehouses(id) ON DELETE CASCADE,
    quantity_on_hand INTEGER NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system',
    CONSTRAINT uq_inv_stock_item_wh UNIQUE (organization_id, item_id, warehouse_id)
);

CREATE TABLE inv_stock_movements (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    item_id UUID NOT NULL REFERENCES inv_items(id),
    warehouse_id UUID NOT NULL REFERENCES inv_warehouses(id),
    movement_type VARCHAR(32) NOT NULL,
    quantity_delta INTEGER NOT NULL,
    reference_note VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system'
);

CREATE TABLE inv_purchase_orders (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    po_number VARCHAR(40) NOT NULL,
    vendor_name VARCHAR(160) NOT NULL,
    warehouse_id UUID NOT NULL REFERENCES inv_warehouses(id),
    item_id UUID NOT NULL REFERENCES inv_items(id),
    quantity INTEGER NOT NULL,
    unit_cost NUMERIC(18, 2) NOT NULL,
    total_cost NUMERIC(18, 2) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ORDERED',
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system',
    CONSTRAINT uq_inv_po_org_num UNIQUE (organization_id, po_number)
);

CREATE TABLE inv_store_orders (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    order_number VARCHAR(40) NOT NULL,
    customer_name VARCHAR(160) NOT NULL,
    customer_email VARCHAR(180) NOT NULL,
    warehouse_id UUID NOT NULL REFERENCES inv_warehouses(id),
    item_id UUID NOT NULL REFERENCES inv_items(id),
    quantity INTEGER NOT NULL,
    total_amount NUMERIC(18, 2) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PAID',
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system',
    CONSTRAINT uq_inv_store_org_num UNIQUE (organization_id, order_number)
);

INSERT INTO permissions (id, module_code, permission_key, description) VALUES
('66666666-6666-6666-6666-666666660001', 'finance', 'finance:read', 'View chart of accounts, journals, invoices, bills, and financial reports'),
('66666666-6666-6666-6666-666666660002', 'finance', 'finance:write', 'Create journals, invoices, vendor bills, and record payments'),
('66666666-6666-6666-6666-666666660003', 'billing', 'billing:read', 'View subscription plans and customer subscriptions'),
('66666666-6666-6666-6666-666666660004', 'billing', 'billing:write', 'Manage subscription plans, upgrades, cancellations, and dunning'),
('66666666-6666-6666-6666-666666660005', 'inventory', 'inventory:read', 'View items, warehouses, stock levels, POs, and store orders'),
('66666666-6666-6666-6666-666666660006', 'inventory', 'inventory:write', 'Adjust/transfer stock, create/receive POs, and process store orders');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.code IN ('ORG_OWNER', 'ORG_ADMIN')
  AND p.module_code IN ('finance', 'billing', 'inventory')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

UPDATE platform_modules SET status = 'ACTIVE' WHERE code IN ('finance', 'inventory');
UPDATE system_settings SET setting_value = '5' WHERE setting_key = 'platform.current.phase';

