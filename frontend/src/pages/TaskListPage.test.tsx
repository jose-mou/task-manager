import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { TaskListPage } from './TaskListPage'
import { seedTask } from '../mocks/handlers'

describe('TaskListPage', () => {
  it('renders a link to create a new task', async () => {
    render(
      <MemoryRouter>
        <TaskListPage />
      </MemoryRouter>,
    )

    expect(
      await screen.findByRole('link', { name: /new task/i }),
    ).toHaveAttribute('href', '/tasks/new')
  })

  it('lists the tasks returned by the API', async () => {
    seedTask({ name: 'Nightly backup' })
    seedTask({ name: 'Report generation' })

    render(
      <MemoryRouter>
        <TaskListPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText('Nightly backup')).toBeInTheDocument()
    expect(screen.getByText('Report generation')).toBeInTheDocument()
  })

  it('shows an empty state when there are no tasks', async () => {
    render(
      <MemoryRouter>
        <TaskListPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText(/no tasks/i)).toBeInTheDocument()
  })
})
