-- Human Resource Management
CREATE TABLE hr_employees (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    employee_code VARCHAR(40) NOT NULL,
    full_name VARCHAR(160) NOT NULL,
    work_email VARCHAR(180) NOT NULL,
    designation VARCHAR(120) NOT NULL,
    department_name VARCHAR(100),
    branch_name VARCHAR(100),
    manager_employee_id UUID REFERENCES hr_employees(id),
    joining_date DATE NOT NULL,
    employment_type VARCHAR(32) NOT NULL DEFAULT 'FULL_TIME',
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    base_monthly_salary NUMERIC(18, 2) NOT NULL DEFAULT 0,
    annual_leave_balance INTEGER NOT NULL DEFAULT 18,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system',
    CONSTRAINT uq_hr_emp_org_code UNIQUE (organization_id, employee_code),
    CONSTRAINT uq_hr_emp_org_email UNIQUE (organization_id, work_email)
);

CREATE TABLE hr_attendance_records (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    employee_id UUID NOT NULL REFERENCES hr_employees(id) ON DELETE CASCADE,
    work_date DATE NOT NULL,
    check_in_at TIMESTAMP WITH TIME ZONE NOT NULL,
    check_out_at TIMESTAMP WITH TIME ZONE,
    work_mode VARCHAR(24) NOT NULL DEFAULT 'OFFICE',
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    worked_minutes INTEGER NOT NULL DEFAULT 0,
    overtime_minutes INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL DEFAULT 'PRESENT',
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system',
    CONSTRAINT uq_hr_att_emp_date UNIQUE (organization_id, employee_id, work_date)
);

CREATE TABLE hr_leave_requests (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    employee_id UUID NOT NULL REFERENCES hr_employees(id) ON DELETE CASCADE,
    leave_type VARCHAR(32) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    days_count INTEGER NOT NULL,
    reason VARCHAR(300),
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    reviewed_by_user_id UUID,
    reviewed_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system'
);

CREATE TABLE hr_performance_goals (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    employee_id UUID NOT NULL REFERENCES hr_employees(id) ON DELETE CASCADE,
    title VARCHAR(180) NOT NULL,
    target_metric VARCHAR(140) NOT NULL,
    progress_percent INTEGER NOT NULL DEFAULT 0,
    review_status VARCHAR(32) NOT NULL DEFAULT 'IN_PROGRESS',
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system'
);

-- Recruitment ATS
CREATE TABLE rec_jobs (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    job_code VARCHAR(40) NOT NULL,
    title VARCHAR(160) NOT NULL,
    department VARCHAR(100),
    location VARCHAR(120),
    employment_type VARCHAR(32) NOT NULL DEFAULT 'FULL_TIME',
    openings_count INTEGER NOT NULL DEFAULT 1,
    status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system',
    CONSTRAINT uq_rec_jobs_org_code UNIQUE (organization_id, job_code)
);

CREATE TABLE rec_candidates (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    job_id UUID NOT NULL REFERENCES rec_jobs(id) ON DELETE CASCADE,
    full_name VARCHAR(160) NOT NULL,
    email VARCHAR(180) NOT NULL,
    phone VARCHAR(50),
    skills VARCHAR(300),
    experience_years INTEGER NOT NULL DEFAULT 0,
    resume_summary TEXT,
    stage VARCHAR(32) NOT NULL DEFAULT 'APPLIED',
    rating INTEGER,
    hired_employee_id UUID REFERENCES hr_employees(id),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system'
);

CREATE TABLE rec_interviews (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    candidate_id UUID NOT NULL REFERENCES rec_candidates(id) ON DELETE CASCADE,
    interviewer_name VARCHAR(150) NOT NULL,
    scheduled_at TIMESTAMP WITH TIME ZONE NOT NULL,
    mode VARCHAR(32) NOT NULL DEFAULT 'VIDEO',
    feedback VARCHAR(500),
    recommendation VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system'
);

-- Payroll
CREATE TABLE pay_runs (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    pay_month VARCHAR(16) NOT NULL,
    employees_count INTEGER NOT NULL DEFAULT 0,
    total_gross NUMERIC(18, 2) NOT NULL DEFAULT 0,
    total_deductions NUMERIC(18, 2) NOT NULL DEFAULT 0,
    total_net_pay NUMERIC(18, 2) NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL DEFAULT 'PROCESSED',
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system',
    CONSTRAINT uq_pay_runs_org_month UNIQUE (organization_id, pay_month)
);

CREATE TABLE pay_payslips (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    payroll_run_id UUID NOT NULL REFERENCES pay_runs(id) ON DELETE CASCADE,
    employee_id UUID NOT NULL REFERENCES hr_employees(id),
    employee_name VARCHAR(160) NOT NULL,
    pay_month VARCHAR(16) NOT NULL,
    basic_pay NUMERIC(18, 2) NOT NULL,
    hra_allowance NUMERIC(18, 2) NOT NULL,
    special_allowance NUMERIC(18, 2) NOT NULL,
    gross_pay NUMERIC(18, 2) NOT NULL,
    pf_deduction NUMERIC(18, 2) NOT NULL,
    esi_deduction NUMERIC(18, 2) NOT NULL,
    tds_tax_deduction NUMERIC(18, 2) NOT NULL,
    total_deductions NUMERIC(18, 2) NOT NULL,
    net_pay NUMERIC(18, 2) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system'
);

INSERT INTO permissions (id, module_code, permission_key, description) VALUES
('77777777-7777-7777-7777-777777770001', 'hr', 'hr:read', 'View employees, attendance, leave requests, and performance goals'),
('77777777-7777-7777-7777-777777770002', 'hr', 'hr:write', 'Manage employee profiles, attendance, leave approvals, and goals'),
('77777777-7777-7777-7777-777777770003', 'recruitment', 'recruitment:read', 'View job openings, candidates, and interviews'),
('77777777-7777-7777-7777-777777770004', 'recruitment', 'recruitment:write', 'Create jobs, advance candidates, schedule interviews, and hire'),
('77777777-7777-7777-7777-777777770005', 'payroll', 'payroll:read', 'View payroll runs and employee payslips'),
('77777777-7777-7777-7777-777777770006', 'payroll', 'payroll:write', 'Process monthly payroll runs and full & final settlements');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.code IN ('ORG_OWNER', 'ORG_ADMIN')
  AND p.module_code IN ('hr', 'recruitment', 'payroll')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

UPDATE platform_modules SET status = 'ACTIVE' WHERE code IN ('hr', 'recruitment', 'payroll');
UPDATE system_settings SET setting_value = '6' WHERE setting_key = 'platform.current.phase';

