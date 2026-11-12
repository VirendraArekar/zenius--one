# Zenius One — 10-Phase Implementation Roadmap

| Phase | Scope | Key Exit Criteria |
|---|---|---|
| **Phase 1 (Current)** | Architecture, Monorepo, Spring Boot 3+ Core, Flyway V1, React 19 + TS + Tailwind + Shadcn UI Shell, Design System, Docker Compose | Compiles cleanly, Flyway runs, System Registry API live, Frontend shell & Theme/Command Palette connected to real backend API, unit & integration tests passing |
| **Phase 2** | Identity, SSO, JWT Rotation, MFA, Organizations, Org Switcher, Employees, Departments, Branches, RBAC, Audit Logs | Full auth flows, multi-tenant isolation tests proving zero cross-tenant leakage, Admin Org/Role UI |
| **Phase 3** | CRM & Sales (Leads, Contacts, Accounts, Pipelines/Kanban Deals, Products, Price Books, Quotes, Orders, Blueprints) | End-to-end Lead-to-Cash CRM flows with backend validation, pagination, tests, and React UI |
| **Phase 4** | Marketing Automation & Customer Support (Campaigns, Journeys, Forms, Tickets, SLA, Knowledge Base, Live Chat) | Multi-channel ticketing with SLA timers, campaign builder & analytics connected to backend |
| **Phase 5** | Finance, Accounting (COA, GL, Journals, AR/AP, India GST/TDS, Reports), Billing & Subscriptions, Inventory & Commerce | Double-entry accounting invariants tested, invoicing/dunning, multi-warehouse stock ledger |
| **Phase 6** | HR Management, Attendance, Leave, Performance, Recruitment (ATS), Payroll (PF/ESI, Payslips) | Payroll batch processing, leave approval workflows, recruitment pipeline |
| **Phase 7** | Projects (Kanban/Gantt/Sprints), Email & Team Collaboration (WebSocket Chat/Meetings), WorkDrive, Office Suite, e-Sign, Legal CLM | Real-time collaboration, file versioning, digital signature audit trail |
| **Phase 8** | Analytics & BI (Dashboard Builder, Pivots, Exports), Low-Code Creator, Workflow Automation & RPA | Visual workflow engine executing Kafka/RabbitMQ triggers, dynamic Creator forms |
| **Phase 9** | Zenius AI Platform (Global Assistant, 11 Tool-Calling Agents with HITL, pgvector RAG, Predictive ML, AI Workflow Gen) | Permission-scoped RAG citations, AI agent approval workflows, streaming chat UI |
| **Phase 10** | Security Audit (OWASP), Performance & Load Testing, CI/CD (GitHub Actions/Jenkins), Kubernetes & AWS Helm/Terraform | Automated security/load gates green, production K8s manifests verified |

