import { configureStore } from '@reduxjs/toolkit'
import { render, screen } from '@testing-library/react'
import { Provider } from 'react-redux'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import authReducer, { setSession } from '@/app/slices/authSlice'
import uiReducer from '@/app/slices/uiSlice'
import { adminApi } from '@/app/api/adminApi'
import { crmApi } from '@/app/api/crmApi'
import { growthAndSupportApi } from '@/app/api/growthAndSupportApi'
import { systemApi } from '@/app/api/systemApi'
import { axiosClient } from '@/app/api/axiosClient'
import { SupportWorkspacePage } from './SupportWorkspacePage'

describe('SupportWorkspacePage', () => {
  it('renders live support metrics and ticket queue', async () => {
    const spy = vi.spyOn(axiosClient, 'request').mockImplementation(async ({ url }) => {
      if (url === '/support/dashboard') {
        return {
          data: {
            success: true,
            data: {
              totalTickets: 3,
              openTickets: 2,
              resolvedTickets: 1,
              escalatedTickets: 0,
              averageCsat: 4.8,
              publishedKbArticles: 5,
              fieldAppointments: 1,
            },
            meta: { timestamp: '2026-11-12T12:00:00Z' },
          },
        }
      }
      if (url === '/support/tickets') {
        return {
          data: {
            success: true,
            data: [
              {
                id: 't1',
                ticketNumber: 'TKT-00001',
                subject: 'SSO Metadata Help',
                description: 'Need SAML XML',
                channel: 'EMAIL',
                priority: 'HIGH',
                status: 'OPEN',
                requesterName: 'Rohan',
                requesterEmail: 'rohan@acme.io',
                slaDueAt: null,
                escalated: false,
                csatRating: null,
                createdAt: '2026-11-12T12:00:00Z',
              },
            ],
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
        [growthAndSupportApi.reducerPath]: growthAndSupportApi.reducer,
      },
      middleware: (g) =>
        g().concat(
          systemApi.middleware,
          adminApi.middleware,
          crmApi.middleware,
          growthAndSupportApi.middleware
        ),
    })

    store.dispatch(
      setSession({
        accessToken: 'jwt',
        refreshToken: 'refresh',
        user: {
          id: 'u1',
          email: 'ops@acme.io',
          fullName: 'Ops User',
          mfaEnabled: false,
          activeOrganizationId: 'org-1',
          permissions: ['support:ticket:read'],
          organizations: [{ id: 'org-1', slug: 'acme', name: 'Acme', baseCurrency: 'USD', timezone: 'UTC' }],
        },
      })
    )

    render(
      <Provider store={store}>
        <MemoryRouter>
          <SupportWorkspacePage />
        </MemoryRouter>
      </Provider>
    )

    expect(await screen.findByText('TKT-00001')).toBeInTheDocument()
    expect(await screen.findByText('4.8')).toBeInTheDocument()

    spy.mockRestore()
  })
})

