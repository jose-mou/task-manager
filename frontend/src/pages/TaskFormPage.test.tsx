import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { TaskFormPage } from './TaskFormPage'
import { seedService, seedTask } from '../mocks/handlers'
import { AuthProvider } from '../auth/AuthContext'
import { loginAs } from '../test/authHelpers'

function renderAt(path: string) {
  return render(
    <AuthProvider>
      <MemoryRouter initialEntries={[path]}>
        <Routes>
          <Route path="/tasks" element={<div>Task list page</div>} />
          <Route path="/login" element={<div>Login page</div>} />
          <Route path="/tasks/new" element={<TaskFormPage />} />
          <Route path="/tasks/:id/edit" element={<TaskFormPage />} />
        </Routes>
      </MemoryRouter>
    </AuthProvider>,
  )
}

describe('TaskFormPage - create', () => {
  it('creates a task and navigates back to the list on success', async () => {
    seedService({ name: 'reporting-service' })
    loginAs('ADMIN')
    const user = userEvent.setup()
    renderAt('/tasks/new')

    await user.type(await screen.findByLabelText(/name/i), 'Report generation')
    await user.selectOptions(screen.getByLabelText(/service/i), 'reporting-service')
    await user.type(screen.getByLabelText(/^script/i), '/opt/scripts/report.sh')
    await user.click(screen.getByRole('button', { name: /save/i }))

    expect(await screen.findByText('Task list page')).toBeInTheDocument()
  })

  it('shows inline errors for every offending field from the 400 response', async () => {
    seedService({ name: 'svc' })
    loginAs('ADMIN')
    const user = userEvent.setup()
    renderAt('/tasks/new')

    await user.selectOptions(await screen.findByLabelText(/service/i), 'svc')
    await user.type(screen.getByLabelText(/^script/i), '/s.sh')
    await user.click(screen.getByLabelText(/scheduled/i))
    await user.click(screen.getByRole('button', { name: /save/i }))

    expect(await screen.findByText('must not be blank')).toBeInTheDocument()
    expect(
      screen.getByText(
        'must be a valid 6-field cron expression when the task is scheduled',
      ),
    ).toBeInTheDocument()
  })

  it('shows an inline cronExpr error for a syntactically invalid cron expression', async () => {
    seedService({ name: 'backup-service' })
    loginAs('ADMIN')
    const user = userEvent.setup()
    renderAt('/tasks/new')

    await user.type(await screen.findByLabelText(/name/i), 'Nightly backup')
    await user.selectOptions(screen.getByLabelText(/service/i), 'backup-service')
    await user.type(screen.getByLabelText(/^script/i), '/opt/scripts/backup.sh')
    await user.click(screen.getByLabelText(/scheduled/i))
    await user.type(screen.getByLabelText(/cron/i), 'not-a-cron')
    await user.click(screen.getByRole('button', { name: /save/i }))

    expect(await screen.findByLabelText(/cron/i)).toHaveAccessibleDescription(
      'must be a valid 6-field cron expression when the task is scheduled',
    )
  })

  it('surfaces the 409 duplicate-name error on the name field', async () => {
    seedService({ name: 'svc' })
    seedTask({ name: 'Backup', service: 'svc' })
    loginAs('ADMIN')
    const user = userEvent.setup()
    renderAt('/tasks/new')

    await user.type(await screen.findByLabelText(/name/i), 'backup')
    await user.selectOptions(screen.getByLabelText(/service/i), 'svc')
    await user.type(screen.getByLabelText(/^script/i), '/s.sh')
    await user.click(screen.getByRole('button', { name: /save/i }))

    expect(
      await screen.findByText(
        "A task with name 'backup' already exists for service 'svc'",
      ),
    ).toBeInTheDocument()
  })

  it('redirects to login when the session is rejected while loading services', async () => {
    // No session: GET /api/services is 401 for an anonymous caller.
    renderAt('/tasks/new')

    expect(await screen.findByText('Login page')).toBeInTheDocument()
  })
})

describe('TaskFormPage - edit', () => {
  it('pre-fills the form with the existing task and updates it on submit', async () => {
    seedService({ name: 'backup-service' })
    const existing = seedTask({
      name: 'Nightly backup',
      service: 'backup-service',
      script: '/opt/scripts/backup.sh',
      status: 'CREATED',
    })
    loginAs('ADMIN')
    const user = userEvent.setup()
    renderAt(`/tasks/${existing.id}/edit`)

    expect(await screen.findByLabelText(/name/i)).toHaveValue('Nightly backup')

    await user.selectOptions(screen.getByLabelText(/status/i), 'COMPLETED')
    await user.click(screen.getByRole('button', { name: /save/i }))

    expect(await screen.findByText('Task list page')).toBeInTheDocument()
  })

  it('shows a not-found message for an unknown task id', async () => {
    loginAs('ADMIN')
    renderAt('/tasks/missing-id/edit')

    expect(await screen.findByText(/not found/i)).toBeInTheDocument()
  })
})
