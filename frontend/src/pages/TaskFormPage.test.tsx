import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { TaskFormPage } from './TaskFormPage'
import { seedTask } from '../mocks/handlers'

function renderAt(path: string) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route path="/tasks" element={<div>Task list page</div>} />
        <Route path="/tasks/new" element={<TaskFormPage />} />
        <Route path="/tasks/:id/edit" element={<TaskFormPage />} />
      </Routes>
    </MemoryRouter>,
  )
}

describe('TaskFormPage - create', () => {
  it('creates a task and navigates back to the list on success', async () => {
    const user = userEvent.setup()
    renderAt('/tasks/new')

    await user.type(screen.getByLabelText(/name/i), 'Report generation')
    await user.type(screen.getByLabelText(/service/i), 'reporting-service')
    await user.type(screen.getByLabelText(/^script/i), '/opt/scripts/report.sh')
    await user.click(screen.getByRole('button', { name: /save/i }))

    expect(await screen.findByText('Task list page')).toBeInTheDocument()
  })

  it('shows inline errors for every offending field from the 400 response', async () => {
    const user = userEvent.setup()
    renderAt('/tasks/new')

    await user.type(screen.getByLabelText(/service/i), 'svc')
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

  it('surfaces the 409 duplicate-name error on the name field', async () => {
    seedTask({ name: 'Backup' })
    const user = userEvent.setup()
    renderAt('/tasks/new')

    await user.type(screen.getByLabelText(/name/i), 'backup')
    await user.type(screen.getByLabelText(/service/i), 'svc')
    await user.type(screen.getByLabelText(/^script/i), '/s.sh')
    await user.click(screen.getByRole('button', { name: /save/i }))

    expect(
      await screen.findByText("A task with name 'backup' already exists"),
    ).toBeInTheDocument()
  })
})

describe('TaskFormPage - edit', () => {
  it('pre-fills the form with the existing task and updates it on submit', async () => {
    const existing = seedTask({
      name: 'Nightly backup',
      service: 'backup-service',
      script: '/opt/scripts/backup.sh',
      status: 'CREATED',
    })
    const user = userEvent.setup()
    renderAt(`/tasks/${existing.id}/edit`)

    expect(await screen.findByLabelText(/name/i)).toHaveValue('Nightly backup')

    await user.selectOptions(screen.getByLabelText(/status/i), 'COMPLETED')
    await user.click(screen.getByRole('button', { name: /save/i }))

    expect(await screen.findByText('Task list page')).toBeInTheDocument()
  })

  it('shows a not-found message for an unknown task id', async () => {
    renderAt('/tasks/missing-id/edit')

    expect(await screen.findByText(/not found/i)).toBeInTheDocument()
  })
})
