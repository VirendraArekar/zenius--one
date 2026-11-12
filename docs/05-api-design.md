# Zenius One — API Design Specification

## 1. Conventions
- **Base Prefix:** `/api/v1`
- **Multi-Tenant Header:** `X-Organization-Id: <uuid>` (validated against the authenticated user's active organization memberships in Phase 2+)
- **Authentication:** `Authorization: Bearer <jwt>` or `X-Api-Key: <key>`
- **Idempotency:** `Idempotency-Key: <uuid>` for mutating financial, billing, and webhook endpoints
- **Pagination:** `?page=0&size=20&sort=createdAt,desc&q=searchTerm`

## 2. Standard Response Envelope

```json
{
  "success": true,
  "data": {},
  "meta": {
    "timestamp": "2026-11-12T10:57:23Z",
    "traceId": "abc123",
    "page": 0,
    "size": 20,
    "totalElements": 120,
    "totalPages": 6
  }
}
```

Errors adhere to RFC 7807 `application/problem+json` with field-level validation details (`errors: [{ field, message }]`).

## 3. Core Endpoint Groups by Bounded Context

| Bounded Context | Base Path | Key Endpoints |
|---|---|---|
| Platform System (Phase 1) | `/api/v1/system` | `GET /status`, `GET /modules`, `GET /modules/{code}` |
| Identity & Auth (Phase 2) | `/api/v1/auth` | `POST /register`, `POST /login`, `POST /refresh`, `POST /logout`, `POST /mfa/verify`, `GET /me` |
| Organizations & RBAC (Phase 2) | `/api/v1/organizations` | `GET/POST /`, `POST /{id}/switch`, `GET/POST /members`, `GET/POST /roles`, `GET/POST /departments`, `GET/POST /branches` |
| CRM & Sales (Phase 3) | `/api/v1/crm` | `/leads`, `/leads/{id}/convert`, `/contacts`, `/accounts`, `/deals`, `/pipelines`, `/quotes`, `/sales-orders` |
| Marketing (Phase 4) | `/api/v1/marketing` | `/campaigns`, `/segments`, `/journeys`, `/landing-pages`, `/forms`, `/social-posts` |
| Support (Phase 4) | `/api/v1/support` | `/tickets`, `/tickets/{id}/comments`, `/sla-policies`, `/kb/articles`, `/service-appointments` |
| Finance & Accounting (Phase 5) | `/api/v1/finance` | `/accounts`, `/journals`, `/invoices`, `/bills`, `/payments`, `/reports/trial-balance`, `/reports/pnl` |
| Billing (Phase 5) | `/api/v1/billing` | `/plans`, `/subscriptions`, `/subscriptions/{id}/upgrade`, `/dunning` |
| Inventory (Phase 5) | `/api/v1/inventory` | `/products`, `/warehouses`, `/stock-transfers`, `/stock-adjustments`, `/purchase-orders` |
| HR, Recruitment, Payroll (Phase 6) | `/api/v1/hr`, `/api/v1/recruitment`, `/api/v1/payroll` | `/employees`, `/attendance/check-in`, `/leaves`, `/jobs`, `/candidates`, `/payroll-runs`, `/payslips` |
| Projects, Collab, Docs, Legal (Phase 7) | `/api/v1/projects`, `/api/v1/collab`, `/api/v1/docs`, `/api/v1/legal` | `/projects`, `/tasks`, `/sprints`, `/channels`, `/mail/messages`, `/files`, `/signatures`, `/contracts` |
| Analytics, Creator, Automation (Phase 8) | `/api/v1/analytics`, `/api/v1/creator`, `/api/v1/automation` | `/dashboards`, `/reports`, `/apps`, `/forms`, `/workflows`, `/workflows/{id}/executions` |
| Zenius AI (Phase 9) | `/api/v1/ai` | `POST /chat`, `POST /agents/{type}/run`, `POST /rag/documents`, `POST /rag/query`, `POST /workflows/generate`, `POST /approvals/{id}/decision` |
| Integrations & Webhooks | `/api/v1/integrations` | `/api-keys`, `/webhooks`, `/connectors` |

