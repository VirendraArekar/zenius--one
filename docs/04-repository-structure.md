# Zenius One — Backend & Frontend Folder Structure

## 1. Monorepo Root Structure

```text
zenius-one/
├── backend/                     # Spring Boot 3.4+ / Java 21+ Modular Monolith (Maven)
├── frontend/                    # React 19+ / TypeScript / Vite Unified SaaS Shell
├── infrastructure/              # Nginx, Prometheus, Grafana, Kubernetes, CI/CD
│   ├── nginx/
│   ├── monitoring/
│   └── k8s/
├── docs/                        # Architecture, ERD, API, Auth, Roadmap specs
├── docker-compose.yml           # Full local stack (Postgres+pgvector, Mongo, Redis, Kafka, Nginx, etc.)
├── .env.example                 # Environment variable contract (no secrets committed)
├── PROJECT_STATUS.md            # Live phase & feature verification tracker
└── README.md                    # Developer quickstart & commands
```

## 2. Backend Folder Structure (`backend/`)

```text
backend/
├── pom.xml
├── Dockerfile
└── src/
    ├── main/
    │   ├── java/com/zenius/one/
    │   │   ├── ZeniusOneApplication.java
    │   │   ├── platform/
    │   │   │   ├── common/              # BaseEntity, ApiResponse, PageResponse, Exceptions, TenantContext
    │   │   │   ├── config/              # SecurityConfig, OpenApiConfig, JpaConfig, CorsConfig, WebSocketConfig
    │   │   │   └── system/              # PlatformModuleRegistry, SystemStatusController, DTOs, Entities
    │   │   └── modules/                 # 20 Domain-Driven Bounded Contexts (activated phase-by-phase)
    │   │       ├── identity/            # Phase 2: SSO, JWT, MFA, Sessions
    │   │       ├── organization/        # Phase 2: Orgs, Branches, Departments, RBAC
    │   │       ├── crm/                 # Phase 3: Leads, Contacts, Accounts, Deals, Quotes
    │   │       ├── marketing/           # Phase 4: Campaigns, Journeys, Forms, Social
    │   │       ├── support/             # Phase 4: Tickets, SLA, KB, Field Service
    │   │       ├── finance/             # Phase 5: COA, GL, Journals, AR/AP, GST/TDS
    │   │       ├── billing/             # Phase 5: Plans, Subscriptions, Dunning
    │   │       ├── inventory/           # Phase 5: SKUs, Warehouses, Stock, E-Commerce
    │   │       ├── hr/                  # Phase 6: Employees, Attendance, Leave, Performance
    │   │       ├── recruitment/         # Phase 6: Jobs, Candidates, ATS, Interviews
    │   │       ├── payroll/             # Phase 6: Salary, Payslips, PF/ESI, Runs
    │   │       ├── projects/            # Phase 7: Projects, Tasks, Sprints, Time
    │   │       ├── collaboration/       # Phase 7: Mail, Chat, Channels, Meetings
    │   │       ├── documents/           # Phase 7: WorkDrive, Office Suite, e-Sign
    │   │       ├── legal/               # Phase 7: Contracts, CLM, Compliance
    │   │       ├── analytics/           # Phase 8: BI Dashboards, Reports, Pivots
    │   │       ├── creator/             # Phase 8: Low-Code Form/App/Page Builder
    │   │       ├── automation/          # Phase 8: Visual Workflows, RPA Bots
    │   │       ├── itsecurity/          # Phase 2/10: Directory, Vault, Assets, Audit
    │   │       ├── ai/                  # Phase 9: Zenius AI, Agents, RAG, Predictions
    │   │       └── integration/         # Phase 2/7: Webhooks, API Keys, OAuth Apps
    │   └── resources/
    │       ├── application.yml
    │       ├── application-dev.yml
    │       ├── application-test.yml
    │       └── db/migration/
    │           └── V1__init_platform_foundation.sql
    └── test/
        └── java/com/zenius/one/
            └── platform/system/         # Unit & Integration tests (JUnit 5, Mockito, MockMvc, Testcontainers)
```

## 3. Frontend Folder Structure (`frontend/`)

```text
frontend/
├── package.json
├── tsconfig.json
├── vite.config.ts
├── vitest.config.ts
├── playwright.config.ts
├── Dockerfile
└── src/
    ├── main.tsx
    ├── App.tsx
    ├── index.css                        # Tailwind CSS + Design System tokens (light/dark)
    ├── app/
    │   ├── store.ts                     # Redux Toolkit store + RTK Query middleware
    │   ├── hooks.ts                     # Typed useAppDispatch / useAppSelector
    │   └── api/
    │       ├── baseApi.ts               # RTK Query baseQuery + Axios client + tenant/auth headers
    │       └── systemApi.ts             # Live platform status & module registry endpoints
    ├── components/
    │   ├── ui/                          # Shadcn-inspired Design System primitives (Button, Card, Badge, Input, Dialog, Table, Tabs...)
    │   └── layout/                      # Unified Enterprise Shell (AppShell, Sidebar, Topbar, AppSwitcher, CommandPalette, ThemeToggle, OrgSwitcher, NotificationCenter)
    ├── features/
    │   ├── platform/                    # Phase 1: Home Overview, App Directory, Design System Showcase
    │   ├── auth/                        # Phase 2: Login, SSO, MFA, Org Onboarding
    │   ├── crm/                         # Phase 3+
    │   └── ...                          # Per-module feature slices matching backend bounded contexts
    ├── routes/
    │   └── router.tsx                   # React Router v7 route tree for all 20 apps
    └── test/
        └── setup.ts                     # Vitest + React Testing Library setup
```

