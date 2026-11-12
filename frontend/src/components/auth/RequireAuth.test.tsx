import { configureStore } from '@reduxjs/toolkit'
import { Provider } from 'react-redux'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import authReducer from '@/app/slices/authSlice'
import uiReducer from '@/app/slices/uiSlice'
import { RequireAuth } from './RequireAuth'

describe('RequireAuth', () => {
  it('redirects unauthenticated users from protected module routes to /login', () => {
    const unauthStore = configureStore({
      reducer: { auth: authReducer, ui: uiReducer },
    })

    render(
      <Provider store={unauthStore}>
        <MemoryRouter initialEntries={['/crm']}>
          <Routes>
            <Route path="/login" element={<div>Zenius Login Gate</div>} />
            <Route element={<RequireAuth />}>
              <Route path="/crm" element={<div>CRM Workspace</div>} />
            </Route>
          </Routes>
        </MemoryRouter>
      </Provider>
    )

    expect(screen.getByText('Zenius Login Gate')).toBeInTheDocument()
    expect(screen.queryByText('CRM Workspace')).not.toBeInTheDocument()
  })
})

