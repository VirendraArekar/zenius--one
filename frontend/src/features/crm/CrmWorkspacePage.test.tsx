import { configureStore } from '@reduxjs/toolkit'
import { render, screen } from '@testing-library/react'
import { Provider } from 'react-redux'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import authReducer, { setSession } from '@/app/slices/authSlice'
import uiReducer from '@/app/slices/uiSlice'
import { adminApi } from '@/app/api/adminApi'
import { crmApi } from '@/app/api/crmApi'
import { systemApi } from '@/app/api/systemApi'
import { axiosClient } from '@/app/api/axiosClient'
import { CrmWorkspacePage } from './CrmWorkspacePage'

describe('CrmWorkspacePage', () => {
  it('renders live CRM dashboard KPIs when authenticated', async () => {
    const spy = vi.spyOn(axiosClient, 'request').mockImplementation(async ({ url }) => {
      if (url === '/crm/dashboard') {
        return {
          data: {
            success: true,
            data: {
              totalLeads: 4,
              convertedLeads: 2,
              totalAccounts: 3,
              totalContacts: 5,
              openDeals: 2,
              openPipelineAmount: 120000,
              weightedForecast: 66000,
              closedWonAmount: 40000,
              stageBreakdown: [{ stageId: '1', stageName: 'Qualification', dealCount: 2, totalAmount: 120000 }],
              leadsBySource: [{ source: 'WEBSITE', leadCount: 4 }],
            },
            meta: { timestamp: '2026-11-12T12:00:00Z' },
          },
        }
      }
      return { data: { success: true, data: [], meta: { timestamp: '2026-11-12T12:00:00Z' } } }
    })

    const store = configureStore({
      reducer: {
        ui: uiReducer,
        auth: authReducer,
        [systemApi.reducerPath]: systemApi.reducer,
        [adminApi.reducerPath]: adminApi.reducer,
        [crmApi.reducerPath]: crmApi.reducer,
      },
      middleware: (g) => g().concat(systemApi.middleware, adminApi.middleware, crmApi.middleware),
    })

    store.dispatch(
      setSession({
        accessToken: 'test-jwt',
        refreshToken: 'test-refresh',
        user: {
          id: 'u1',
          email: 'alice@orga.test',
          fullName: 'Alice Owner',
          mfaEnabled: false,
          activeOrganizationId: 'org-1',
          permissions: ['crm:lead:read', 'crm:deal:read'],
          organizations: [{ id: 'org-1', slug: 'org-a', name: 'Org Alpha', baseCurrency: 'USD', timezone: 'UTC' }],
        },
      })
    )

    render(
      <Provider store={store}>
        <MemoryRouter>
          <CrmWorkspacePage />
        </MemoryRouter>
      </Provider>
    )

    expect(await screen.findByText('$120,000')).toBeInTheDocument()
    expect(await screen.findByText('$66,000')).toBeInTheDocument()

    spy.mockRestore()
  })
})

