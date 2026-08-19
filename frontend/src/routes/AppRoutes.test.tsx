import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { AppRoutes } from './AppRoutes'

describe('AppRoutes', () => {
  it('shows a navigation entry linking to the task list', async () => {
    render(
      <MemoryRouter initialEntries={['/tasks']}>
        <AppRoutes />
      </MemoryRouter>,
    )

    expect(
      await screen.findByRole('link', { name: /^tasks$/i }),
    ).toHaveAttribute('href', '/tasks')
  })

  it('redirects the root path to the task list', async () => {
    render(
      <MemoryRouter initialEntries={['/']}>
        <AppRoutes />
      </MemoryRouter>,
    )

    expect(await screen.findByRole('heading', { name: /tasks/i })).toBeInTheDocument()
  })

  it('renders the create form at /tasks/new', async () => {
    render(
      <MemoryRouter initialEntries={['/tasks/new']}>
        <AppRoutes />
      </MemoryRouter>,
    )

    expect(
      await screen.findByRole('heading', { name: /new task/i }),
    ).toBeInTheDocument()
  })
})
