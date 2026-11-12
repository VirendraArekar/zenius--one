# Zenius One — Application Module Breakdown & Feature Checklist

This document decomposes all 20 bounded contexts and tracks implementation status against strict completion criteria (Frontend + Backend + Database + Authorization + Tests verified).

## Legend
- `[ ]` Not started
- `[~]` In progress / Foundation only
- `[x]` Verified complete (DB + Backend + Security + Frontend + Tests)

---

## 0. Global Platform Core & Infrastructure (Phase 1 & Phase 2)
- [x] Monorepo, Docker Compose (Postgres+pgvector, MongoDB, Redis, Kafka, Nginx, Prometheus, Grafana) (Phase 1)
- [x] Modular Spring Boot 3.5+ / Java 21+ core, Flyway migrations, Global Exception Handling, OpenAPI 3, Actuator (Phase 1)
- [x] React 19 + TS + Tailwind + Shadcn UI Design System, App Shell, Sidebar, Theme Switcher, Command Palette (Phase 1)
- [x] Platform System Registry & Live Health Dashboard backed by database & REST APIs (Phase 1)
- [x] SSO & JWT Authentication with Refresh Token Rotation & Revocation (Phase 2)
- [x] MFA (TOTP), Active Session Management & Scoped API Keys (Phase 2)
- [x] Multi-Organization Provisioning & Live Organization Switcher (Phase 2)
- [x] Employees/Members, Departments, Branches & Locations Management (Phase 2)
- [x] RBAC & Fine-Grained Permission Matrix (`@authz.hasPermission`) (Phase 2)
- [x] Centralized Notification Center & Immutable Audit Logs (Phase 2)
- [ ] Global Search (Elasticsearch) (Phase 2+)
- [ ] Application Marketplace, Subscriptions, Billing & Licensing (Phase 2/5)
- [ ] Webhooks & Third-Party OAuth Integrations (Phase 7)

---

## 1. Sales & CRM (`crm`) — Phase 3
- [x] Leads: CRUD, source tracking, scoring, qualification, conversion, duplicate email detection, ownership, CSV import/export
- [x] Contacts: CRUD, account linkage, segmentation
- [x] Accounts: Company profiles, parent hierarchy, ownership, annual revenue, lifecycle stages
- [x] Deals: Default pipeline provisioning, stages, probability, forecasting, expected revenue, lost reason tracking, Kanban board
- [x] Commercial Docs: Products and services catalog, multi-line quotes with discount/tax calculation, sales orders (quote-to-order conversion)
- [x] CRM Core Activities & Analytics: Sales activities (calls/meetings/tasks), live funnel & forecast dashboard
- [ ] Extended CRM (subsequent phases): Price books, invoices/POs (Phase 5 Finance/Inventory), contracts (Phase 7 Legal), Blueprints/custom modules (Phase 8), territory management, gamification

## 2. Marketing Automation (`marketing`) — Phase 4
- [x] Email marketing campaigns, draft/launch lifecycle, audience dispatch, and performance analytics
- [x] Subscribers, dynamic tag/source segmentation, and customer drip journeys
- [x] Website lead-capture forms (auto-provisioning CRM leads & marketing subscribers)
- [x] Coupons & promotional discount codes
- [ ] Extended Marketing (subsequent phases): Social publishing calendar/listening, heatmaps, A/B testing, webinars/events, referral tracking

## 3. Customer Support & Service (`support`) — Phase 4
- [x] Multi-channel tickets (`TKT-*`), priority queues, status transitions, and threaded replies/internal notes
- [x] Knowledge base & FAQ articles with helpful voting and search
- [x] Auto-provisioned SLA policies, due deadlines, escalation flag, and canned responses
- [x] CSAT ratings & feedback, support analytics dashboard
- [x] Field service appointments (`FSV-*`) and technician dispatch scheduling
- [ ] Extended Support (subsequent phases): Live WebSocket chat/call center, remote screen sharing, customer portal, service inventory & warranty tracking

## 4. Finance & Accounting (`finance`) — Phase 5
- [x] Chart of accounts (auto-provisioned 10 system accounts), general ledger, balanced double-entry journal entries, trial balance, P&L & balance summary
- [x] India GST output tracking & TDS deduction tracking across invoices and vendor bills
- [x] Accounts Receivable: customer invoices (`INV-*`), automatic GL posting, customer payments & balance tracking
- [x] Accounts Payable: vendor bills (`BILL-*`), automatic GL posting, vendor payments & TDS tracking
- [ ] Extended Finance (subsequent phases): Bank reconciliation, multi-currency FX rates, recurring invoices, fixed asset depreciation, budgets & cost centers

## 5. Billing & Subscriptions (`billing`) — Phase 5
- [x] Subscription plans, monthly/yearly billing intervals, and free trials
- [x] Plan upgrades/downgrades with proration credit calculation and cancellations
- [ ] Extended Billing (subsequent phases): Usage-based metering, payment gateway webhooks, automated dunning retries & customer billing portal

## 6. Inventory & Commerce (`inventory`) — Phase 5
- [x] Item catalog, SKUs, reorder thresholds, multi-warehouse support, and stock movement ledger
- [x] Stock adjustments, inter-warehouse transfers, purchase orders (`PO-*`) + receiving, and oversell guards
- [x] Online store order checkout (`ORD-*`) with real-time stock deduction
- [ ] Extended Inventory & Commerce (subsequent phases): Batch/serial numbers, barcode generation/scanning, POS integration, and returns/refunds

## 7. Human Resource Management (`hr`) — Phase 6
- [x] Employee profiles, directory, designations, joining details, salary & leave balances, onboarding & offboarding
- [x] Attendance: daily check-in/out with geo-location coordinates, work mode (`OFFICE`/`REMOTE`), worked minutes & overtime calculation
- [x] Leave management: leave types, balance validation, requests, and approval/rejection workflows
- [x] Performance: employee KPIs and goal tracking
- [ ] Extended HR (subsequent phases): Biometric hardware sync, shift rosters, holiday calendars, 360 appraisals & training LMS

## 8. Recruitment (`recruitment`) — Phase 6
- [x] Job openings (`JOB-*`), candidate database, ATS stage transitions, interview scheduling, and Hire-to-HR onboarding
- [ ] Extended Recruitment (subsequent phases): Resume file upload & AI parsing (Phase 9), offer letter e-Sign (Phase 7)

## 9. Payroll (`payroll`) — Phase 6
- [x] Salary components (Basic, HRA, Special Allowance), automated monthly payroll runs, payslips, India PF/ESI/TDS deductions, and F&F settlement
- [ ] Extended Payroll (subsequent phases): Form-16 / statutory challan PDFs & bank payout file export

## 10. Project Management (`projects`) — Phase 7 ✅
- [x] Projects, sprints, tasks, Kanban task board with status transitions, story points, time tracking & timesheets (core implemented in Phase 7)
- [ ] Extended Project Management (subsequent phases): Gantt charts, subtasks, task dependencies, project expenses & profitability reports

## 11. Email & Collaboration (`collaboration`) — Phase 7 ✅
- [x] Business email: inbox/sent mailboxes, compose/send, labels (core implemented in Phase 7)
- [x] Team communication: team channels, channel messaging, scheduled meetings & join links (core implemented in Phase 7)
- [ ] Extended Collaboration (subsequent phases): live WebRTC screen sharing, IMAP/SMTP external sync, and email filter rules

## 12. Document & Office Suite (`documents`) — Phase 7 ✅
- [x] WorkDrive: auto-provisioned Shared Drive & custom folders, document storage, retention policies (core implemented in Phase 7)
- [x] Office Suite: Writer/Sheet/Slide/Note document editor with automatic revision history snapshots (core implemented in Phase 7)
- [x] Digital Signatures: signature requests, SHA-256 cryptographic signing, audit trail (core implemented in Phase 7)
- [ ] Extended Office/WorkDrive (subsequent phases): real-time CRDT multi-cursor collaboration & PDF annotation renderer

## 13. Legal & Contract Management (`legal`) — Phase 7 ✅
- [x] Contract creation (`CTR-*`), CLM lifecycle (`DRAFT` -> `IN_REVIEW` -> `APPROVED` -> `EXECUTED` -> `EXPIRED`), linked documents, audit trail (core implemented in Phase 7)
- [ ] Extended CLM (subsequent phases): automated renewal alert jobs & clause redline comparison

## 14. Analytics & Business Intelligence (`analytics`) — Phase 8 ✅
- [x] Executive BI pulse across CRM, Finance, Support, HR & Projects, custom dashboards, reports & CSV exports (core implemented in Phase 8)
- [ ] Extended BI (subsequent phases): scheduled email PDF snapshots & SQL/JDBC external data connectors

## 15. Low-Code Platform (`creator`) — Phase 8 ✅
- [x] Custom low-code apps (`APP-*`), dynamic form schema builder, required-field validation, formula calculation fields, records (`REC-*`), and versioned publishing (core implemented in Phase 8)
- [ ] Extended Creator (subsequent phases): drag-and-drop canvas designer & Deluge/sandboxed script runtime

## 16. Workflow Automation & RPA (`automation`) — Phase 8 ✅
- [x] Event-driven workflow rules, condition evaluator (`==`, `>`, `<`), notification & webhook action handlers, toggle controls & execution telemetry logs (core implemented in Phase 8)
- [ ] Extended Automation (subsequent phases): visual node graph editor & distributed Kafka/Quartz cron RPA workers

## 17. IT Management & Security (`itsecurity`) — Phase 2 & Phase 10 ✅
- [x] User directory, centralized identity, SSO, MFA, sessions, API key controls & security audit logs (implemented in Phase 2)
- [x] IT asset inventory (`AST-*`), security incident response (`INC-*` -> `RESOLVED`), org security policies (MFA mandate, password length, CIDR allowlist), and hardened HTTP security headers (implemented in Phase 10)

## 18. Zenius AI (`ai`) — Phase 9 ✅
- [x] Global Conversational AI Assistant (permission-aware across all apps, optional Ollama / OpenAI-compatible backend)
- [x] 11 Specialized AI Agents (`SALES`, `CRM`, `FINANCE`, `HR`, `RECRUITMENT`, `MARKETING`, `SUPPORT`, `PROJECT`, `ANALYTICS`, `DOCUMENT`, `WORKFLOW`) with HITL action approval
- [x] AI CRM, Finance, Support & HR predictive insights computed from live tenant telemetry
- [x] RAG Knowledge Assistant (chunking, vector similarity retrieval, policy citations, org-isolated KBs)
- [x] AI Workflow Generation (natural language prompt to HITL-approved workflow execution)
- [ ] Extended AI (subsequent phases): multimodal document/receipt OCR ingestion & streaming SSE token output

