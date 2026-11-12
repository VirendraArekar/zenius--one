import { configureStore } from '@reduxjs/toolkit'
import { render, screen } from '@testing-library/react'
import { Provider } from 'react-redux'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import authReducer from '@/app/slices/authSlice'
import uiReducer from '@/app/slices/uiSlice'
import { adminApi } from '@/app/api/adminApi'
import { systemApi } from '@/app/api/systemApi'
import { AdminConsolePage } from './AdminConsolePage'

describe('AdminConsolePage', () => {
  it('shows sign-in prompt when unauthenticated', () => {
    localStorage.clear()
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
          <AdminConsolePage />
        </MemoryRouter>
      </Provider>
    )

    expect(screen.getByText('Authentication Required')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Sign In' })).toBeInTheDocument()
  })
})

