import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { TaskListPage } from './TaskListPage'
import { seedTask } from '../mocks/handlers'
import { AuthProvider } from '../auth/AuthContext'
import { loginAs } from '../test/authHelpers'

function renderPage() {
  return render(
    <AuthProvider>
      <MemoryRouter>
        <TaskListPage />
      </MemoryRouter>
    </AuthProvider>,
  )
}

describe('TaskListPage', () => {
  it('does not show a "New task" link for an anonymous visitor', async () => {
    renderPage()

    expect(await screen.findByRole('heading', { name: /tasks/i })).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: /new task/i })).not.toBeInTheDocument()
  })

  it('shows a "New task" link for an ADMIN', async () => {
    loginAs('ADMIN')
    renderPage()

    expect(
      await screen.findByRole('link', { name: /new task/i }),
    ).toHaveAttribute('href', '/tasks/new')
  })

  it('lists the tasks returned by the API, readable without login', async () => {
    seedTask({ name: 'Nightly backup' })
    seedTask({ name: 'Report generation' })

    renderPage()

    expect(await screen.findByText('Nightly backup')).toBeInTheDocument()
    expect(screen.getByText('Report generation')).toBeInTheDocument()
  })

  it('shows an empty state when there are no tasks', async () => {
    renderPage()

    expect(await screen.findByText(/no tasks/i)).toBeInTheDocument()
  })

  it('filters the list by the typed service name', async () => {
    seedTask({ name: 'Backup job', service: 'backup-service' })
    seedTask({ name: 'Report job', service: 'reporting-service' })
    const user = userEvent.setup()

    renderPage()
    await screen.findByText('Backup job')

    await user.type(screen.getByLabelText(/filter by service/i), 'backup-service')

    expect(await screen.findByText('Backup job')).toBeInTheDocument()
    expect(screen.queryByText('Report job')).not.toBeInTheDocument()
  })

  it('treats a blank filter as no filter instead of querying a blank service name', async () => {
    seedTask({ name: 'Backup job', service: 'backup-service' })
    const user = userEvent.setup()

    renderPage()
    await screen.findByText('Backup job')

    await user.type(screen.getByLabelText(/filter by service/i), '   ')

    expect(await screen.findByText('Backup job')).toBeInTheDocument()
  })

  it('says the filter matched nothing rather than that there are no tasks', async () => {
    seedTask({ name: 'Backup job', service: 'backup-service' })
    const user = userEvent.setup()

    renderPage()
    await screen.findByText('Backup job')

    await user.type(screen.getByLabelText(/filter by service/i), 'unknown-service')

    expect(
      await screen.findByText(/no tasks owned by "unknown-service"/i),
    ).toBeInTheDocument()
  })
})
