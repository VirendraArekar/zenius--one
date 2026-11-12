import { configureStore } from '@reduxjs/toolkit'
import { render, screen } from '@testing-library/react'
import { Provider } from 'react-redux'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import authReducer, { setSession } from '@/app/slices/authSlice'
import uiReducer from '@/app/slices/uiSlice'
import { adminApi } from '@/app/api/adminApi'
import { crmApi } from '@/app/api/crmApi'
import { erpApi } from '@/app/api/erpApi'
import { growthAndSupportApi } from '@/app/api/growthAndSupportApi'
import { systemApi } from '@/app/api/systemApi'
import { axiosClient } from '@/app/api/axiosClient'
import { FinanceWorkspacePage } from './FinanceWorkspacePage'

describe('FinanceWorkspacePage', () => {
  it('displays live financial reports and balanced trial balance', async () => {
    const spy = vi.spyOn(axiosClient, 'request').mockImplementation(async ({ url }) => {
      if (url === '/finance/reports') {
        return {
          data: {
            success: true,
            data: {
              totalDebits: 1180,
              totalCredits: 1180,
              totalRevenue: 1000,
              totalExpense: 200,
              netProfit: 800,
              totalAssets: 1180,
              totalLiabilities: 180,
              arOutstanding: 0,
              apOutstanding: 0,
              totalGstCollected: 180,
              totalTdsDeducted: 20,
              trialBalance: [
                {
                  accountId: 'a1',
                  code: '1000',
                  name: 'Cash & Bank',
                  accountType: 'ASSET',
                  debit: 1180,
                  credit: 0,
                  netBalance: 1180,
                },
              ],
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
        [growthAndSupportApi.reducerPath]: growthAndSupportApi.reducer,
        [erpApi.reducerPath]: erpApi.reducer,
      },
      middleware: (g) =>
        g().concat(
          systemApi.middleware,
          adminApi.middleware,
          crmApi.middleware,
          growthAndSupportApi.middleware,
          erpApi.middleware
        ),
    })

    store.dispatch(
      setSession({
        accessToken: 'jwt',
        refreshToken: 'refresh',
        user: {
          id: 'u1',
          email: 'cfo@acme.io',
          fullName: 'CFO',
          mfaEnabled: false,
          activeOrganizationId: 'org-1',
          permissions: ['finance:read'],
          organizations: [{ id: 'org-1', slug: 'acme', name: 'Acme', baseCurrency: 'USD', timezone: 'UTC' }],
        },
      })
    )

    render(
      <Provider store={store}>
        <MemoryRouter>
          <FinanceWorkspacePage />
        </MemoryRouter>
      </Provider>
    )

    expect(await screen.findByText('$1,000')).toBeInTheDocument()
    expect(await screen.findByText('Cash & Bank')).toBeInTheDocument()

    spy.mockRestore()
  })
})

