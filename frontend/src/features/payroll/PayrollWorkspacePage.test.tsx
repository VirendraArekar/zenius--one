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
import { peopleApi } from '@/app/api/peopleApi'
import { systemApi } from '@/app/api/systemApi'
import { axiosClient } from '@/app/api/axiosClient'
import { PayrollWorkspacePage } from './PayrollWorkspacePage'

describe('PayrollWorkspacePage', () => {
  it('displays live payroll runs and payslip breakdown', async () => {
    const spy = vi.spyOn(axiosClient, 'request').mockImplementation(async ({ url }) => {
      if (url === '/payroll/runs') {
        return {
          data: {
            success: true,
            data: [
              {
                id: 'r1',
                payMonth: '2026-11',
                employeesCount: 1,
                totalGross: 100000,
                totalDeductions: 16000,
                totalNetPay: 84000,
                status: 'PROCESSED',
              },
            ],
            meta: { timestamp: '2026-11-12T12:00:00Z' },
          },
        }
      }
      if (url === '/payroll/payslips') {
        return {
          data: {
            success: true,
            data: [
              {
                id: 'p1',
                payrollRunId: 'r1',
                employeeId: 'e1',
                employeeName: 'Ananya Rao',
                payMonth: '2026-11',
                basicPay: 50000,
                hraAllowance: 25000,
                specialAllowance: 25000,
                grossPay: 100000,
                pfDeduction: 6000,
                esiDeduction: 0,
                tdsTaxDeduction: 10000,
                totalDeductions: 16000,
                netPay: 84000,
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
        [erpApi.reducerPath]: erpApi.reducer,
        [peopleApi.reducerPath]: peopleApi.reducer,
      },
      middleware: (g) =>
        g().concat(
          systemApi.middleware,
          adminApi.middleware,
          crmApi.middleware,
          growthAndSupportApi.middleware,
          erpApi.middleware,
          peopleApi.middleware
        ),
    })

    store.dispatch(
      setSession({
        accessToken: 'jwt',
        refreshToken: 'refresh',
        user: {
          id: 'u1',
          email: 'hr@acme.io',
          fullName: 'HR',
          mfaEnabled: false,
          activeOrganizationId: 'org-1',
          permissions: ['payroll:read'],
          organizations: [{ id: 'org-1', slug: 'acme', name: 'Acme', baseCurrency: 'USD', timezone: 'UTC' }],
        },
      })
    )

    render(
      <Provider store={store}>
        <MemoryRouter>
          <PayrollWorkspacePage />
        </MemoryRouter>
      </Provider>
    )

    expect(await screen.findByText('2026-11 Payroll')).toBeInTheDocument()
    expect(await screen.findByText('Ananya Rao')).toBeInTheDocument()
    expect(await screen.findByText('$84,000')).toBeInTheDocument()

    spy.mockRestore()
  })
})

