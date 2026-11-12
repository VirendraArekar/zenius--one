import { configureStore } from '@reduxjs/toolkit'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Provider } from 'react-redux'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import authReducer from '@/app/slices/authSlice'
import uiReducer from '@/app/slices/uiSlice'
import { adminApi } from '@/app/api/adminApi'
import { systemApi } from '@/app/api/systemApi'
import { axiosClient } from '@/app/api/axiosClient'
import { LoginPage } from './LoginPage'

describe('LoginPage', () => {
  it('transitions to MFA verification step when backend returns mfaRequired', async () => {
    const user = userEvent.setup()
    const spy = vi.spyOn(axiosClient, 'request').mockResolvedValueOnce({
      data: {
        success: true,
        data: {
          mfaRequired: true,
          mfaToken: 'challenge-token-123',
          session: null,
        },
        meta: { timestamp: '2026-11-12T12:00:00Z' },
      },
    })

    const store = configureStore({
      reducer: {
        ui: uiReducer,
        auth: authReducer,
        [systemApi.reducerPath]: systemApi.reducer,
        [adminApi.reducerPath]: adminApi.reducer,
      },
      middleware: (g) => g().concat(systemApi.middleware, adminApi.middleware),
    })

    render(
      <Provider store={store}>
        <MemoryRouter>
          <LoginPage />
        </MemoryRouter>
      </Provider>
    )

    await user.type(screen.getByPlaceholderText('admin@zeniusone.local'), 'alice@orga.test')
    await user.type(screen.getByPlaceholderText('••••••••'), 'StrongPass@123')
    await user.click(screen.getByRole('button', { name: 'Sign In' }))

    expect(await screen.findByText('Two-Factor Verification')).toBeInTheDocument()
    expect(screen.getByPlaceholderText('123456')).toBeInTheDocument()

    spy.mockRestore()
  })
})

