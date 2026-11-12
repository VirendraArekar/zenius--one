import { configureStore } from '@reduxjs/toolkit'
import { render, screen } from '@testing-library/react'
import { Provider } from 'react-redux'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import uiReducer from '@/app/slices/uiSlice'
import authReducer from '@/app/slices/authSlice'
import { systemApi } from '@/app/api/systemApi'
import { intelligenceApi } from '@/app/api/intelligenceApi'
import { axiosClient } from '@/app/api/axiosClient'
import { HomeDashboardPage } from './HomeDashboardPage'

describe('HomeDashboardPage', () => {
  it('displays live platform metrics returned from backend API', async () => {
    const spy = vi.spyOn(axiosClient, 'request').mockImplementation(async ({ url }) => {
      if (url === '/system/status') {
        return {
          data: {
            success: true,
            data: {
              platformName: 'Zenius One',
              version: '0.1.0-PHASE1',
              currentPhase: 1,
              status: 'OPERATIONAL',
              databaseProduct: 'PostgreSQL 16.4',
              totalModules: 20,
              activeModules: 1,
              plannedModules: 19,
              serverTime: '2026-11-12T11:00:00Z',
              publicSettings: [
                {
                  key: 'platform.name',
                  value: 'Zenius One',
                  category: 'GENERAL',
                  description: 'Product display name',
                },
              ],
            },
            meta: { timestamp: '2026-11-12T11:00:00Z' },
          },
        }
      }
      return {
        data: {
          success: true,
          data: [
            {
              id: '1',
              code: 'home',
              name: 'Home',
              tagline: 'Unified Command Center',
              description: 'Executive overview',
              category: 'PLATFORM',
              phaseNumber: 1,
              status: 'ACTIVE',
              routePath: '/',
              iconName: 'LayoutDashboard',
              sortOrder: 1,
              boundedContext: 'platform',
            },
          ],
          meta: { timestamp: '2026-11-12T11:00:00Z', totalElements: 1 },
        },
      }
    })

    const store = configureStore({
      reducer: {
        ui: uiReducer,
        auth: authReducer,
        [systemApi.reducerPath]: systemApi.reducer,
        [intelligenceApi.reducerPath]: intelligenceApi.reducer,
      },
      middleware: (g) => g().concat(systemApi.middleware, intelligenceApi.middleware),
    })

    render(
      <Provider store={store}>
        <MemoryRouter>
          <HomeDashboardPage />
        </MemoryRouter>
      </Provider>
    )

    expect(await screen.findByText('OPERATIONAL')).toBeInTheDocument()
    expect((await screen.findAllByText('PostgreSQL 16.4')).length).toBeGreaterThan(0)
    expect(await screen.findByText('platform.name')).toBeInTheDocument()

    spy.mockRestore()
  })
})

