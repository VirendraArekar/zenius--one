import { Provider } from 'react-redux'
import { MemoryRouter } from 'react-router-dom'
import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { store } from '@/app/store'
import { ProjectsWorkspacePage } from './ProjectsWorkspacePage'

describe('ProjectsWorkspacePage', () => {
  it('prompts unauthenticated users to sign in to access project management', () => {
    render(
      <Provider store={store}>
        <MemoryRouter>
          <ProjectsWorkspacePage />
        </MemoryRouter>
      </Provider>
    )

    expect(screen.getByText(/Sign In to Access Project Management/i)).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /Sign In/i })).toHaveAttribute('href', '/login')
  })
})

