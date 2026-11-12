import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { Button } from './Button'

describe('Button', () => {
  it('renders label and handles clicks', async () => {
    const user = userEvent.setup()
    const onClick = vi.fn()
    render(<Button onClick={onClick}>Launch Module</Button>)

    const btn = screen.getByRole('button', { name: 'Launch Module' })
    await user.click(btn)

    expect(btn).toBeInTheDocument()
    expect(onClick).toHaveBeenCalledTimes(1)
  })
})

