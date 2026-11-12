import { Provider } from 'react-redux'
import { MemoryRouter } from 'react-router-dom'
import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { store } from '@/app/store'
import { AnalyticsWorkspacePage } from './AnalyticsWorkspacePage'

describe('AnalyticsWorkspacePage', () => {
  it('prompts unauthenticated users to sign in to access Analytics & BI', () => {
    render(
      <Provider store={store}>
        <MemoryRouter>
          <AnalyticsWorkspacePage />
        </MemoryRouter>
      </Provider>
    )

    expect(screen.getByText(/Sign In to Access Analytics & BI/i)).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /Sign In/i })).toHaveAttribute('href', '/login')
  })
})

