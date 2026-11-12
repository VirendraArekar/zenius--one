import { createBrowserRouter, Navigate } from 'react-router-dom'
import { RequireAuth } from '@/components/auth/RequireAuth'
import { AppShell } from '@/components/layout/AppShell'
import { AdminConsolePage } from '@/features/admin/AdminConsolePage'
import { LoginPage } from '@/features/auth/LoginPage'
import { RegisterPage } from '@/features/auth/RegisterPage'
import { CrmWorkspacePage } from '@/features/crm/CrmWorkspacePage'
import { SalesWorkspacePage } from '@/features/crm/SalesWorkspacePage'
import { FinanceWorkspacePage } from '@/features/finance/FinanceWorkspacePage'
import { HrWorkspacePage } from '@/features/hr/HrWorkspacePage'
import { InventoryWorkspacePage } from '@/features/inventory/InventoryWorkspacePage'
import { MarketingWorkspacePage } from '@/features/marketing/MarketingWorkspacePage'
import { PayrollWorkspacePage } from '@/features/payroll/PayrollWorkspacePage'
import { HomeDashboardPage } from '@/features/platform/HomeDashboardPage'
import { ModulePreviewPage } from '@/features/platform/ModulePreviewPage'
import { RecruitmentWorkspacePage } from '@/features/recruitment/RecruitmentWorkspacePage'
import { SupportWorkspacePage } from '@/features/support/SupportWorkspacePage'
import { ProjectsWorkspacePage } from '@/features/projects/ProjectsWorkspacePage'
import { CollaborationWorkspacePage } from '@/features/collaboration/CollaborationWorkspacePage'
import { DocumentsWorkspacePage } from '@/features/documents/DocumentsWorkspacePage'
import { AnalyticsWorkspacePage } from '@/features/analytics/AnalyticsWorkspacePage'
import { CreatorWorkspacePage } from '@/features/creator/CreatorWorkspacePage'
import { AutomationWorkspacePage } from '@/features/automation/AutomationWorkspacePage'
import { AiWorkspacePage } from '@/features/ai/AiWorkspacePage'

const PLANNED_MODULE_CODES: readonly string[] = []

export const router = createBrowserRouter([
  { path: '/login', element: <LoginPage /> },
  { path: '/register', element: <RegisterPage /> },
  {
    element: <RequireAuth />,
    children: [
      {
        path: '/',
        element: <AppShell />,
        children: [
          { index: true, element: <HomeDashboardPage /> },
          { path: 'apps', element: <Navigate to="/" replace /> },
          { path: 'design-system', element: <Navigate to="/" replace /> },
          { path: 'admin', element: <AdminConsolePage /> },
          { path: 'admin/security', element: <AdminConsolePage /> },
          { path: 'documents', element: <DocumentsWorkspacePage initialTab="drive" /> },
          { path: 'crm', element: <CrmWorkspacePage /> },
          { path: 'sales', element: <SalesWorkspacePage /> },
          { path: 'marketing', element: <MarketingWorkspacePage /> },
          { path: 'support', element: <SupportWorkspacePage /> },
          { path: 'finance', element: <FinanceWorkspacePage /> },
          { path: 'inventory', element: <InventoryWorkspacePage /> },
          { path: 'hr', element: <HrWorkspacePage /> },
          { path: 'recruitment', element: <RecruitmentWorkspacePage /> },
          { path: 'payroll', element: <PayrollWorkspacePage /> },
          { path: 'projects', element: <ProjectsWorkspacePage /> },
          { path: 'collaboration', element: <CollaborationWorkspacePage initialTab="channels" /> },
          { path: 'mail', element: <CollaborationWorkspacePage initialTab="mail" /> },
          { path: 'workdrive', element: <DocumentsWorkspacePage initialTab="drive" /> },
          { path: 'office', element: <DocumentsWorkspacePage initialTab="drive" /> },
          { path: 'analytics', element: <AnalyticsWorkspacePage /> },
          { path: 'creator', element: <CreatorWorkspacePage /> },
          { path: 'automation', element: <AutomationWorkspacePage /> },
          { path: 'ai', element: <AiWorkspacePage /> },
          ...PLANNED_MODULE_CODES.map((code) => ({
            path: code,
            element: <ModulePreviewPage code={code} />,
          })),
        ],
      },
    ],
  },
  { path: '*', element: <Navigate to="/" replace /> },
])
