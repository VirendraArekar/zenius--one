import { Provider } from 'react-redux'
import { MemoryRouter } from 'react-router-dom'
import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { store } from '@/app/store'
import { AiWorkspacePage } from './AiWorkspacePage'

describe('AiWorkspacePage', () => {
  it('prompts unauthenticated users to sign in to access Zenius AI Copilot', () => {
    render(
      <Provider store={store}>
        <MemoryRouter>
          <AiWorkspacePage />
        </MemoryRouter>
      </Provider>
    )

    expect(screen.getByText(/Sign In to Access Zenius AI Copilot/i)).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /Sign In/i })).toHaveAttribute('href', '/login')
  })
})

