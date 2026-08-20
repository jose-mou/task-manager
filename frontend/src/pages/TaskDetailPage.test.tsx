import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { TaskDetailPage } from './TaskDetailPage'
import { seedTask } from '../mocks/handlers'
import { AuthProvider } from '../auth/AuthContext'
import { loginAs } from '../test/authHelpers'

function renderAt(path: string) {
  return render(
    <AuthProvider>
      <MemoryRouter initialEntries={[path]}>
        <Routes>
          <Route path="/tasks/:id" element={<TaskDetailPage />} />
        </Routes>
      </MemoryRouter>
    </AuthProvider>,
  )
}

describe('TaskDetailPage', () => {
  it('shows the full task details for an anonymous visitor, without login', async () => {
    const task = seedTask({
      name: 'Nightly backup',
      service: 'backup-service',
      script: '/opt/scripts/backup.sh',
      description: 'Full database dump.',
      status: 'RUNNING',
    })

    renderAt(`/tasks/${task.id}`)

    expect(await screen.findByRole('heading', { name: 'Nightly backup' })).toBeInTheDocument()
    expect(screen.getByText('backup-service')).toBeInTheDocument()
    expect(screen.getByText('RUNNING')).toBeInTheDocument()
    expect(screen.getByText('/opt/scripts/backup.sh')).toBeInTheDocument()
    expect(screen.getByText('Full database dump.')).toBeInTheDocument()
  })

  it('hides the edit link for an anonymous visitor', async () => {
    const task = seedTask({ name: 'Nightly backup' })

    renderAt(`/tasks/${task.id}`)

    await screen.findByRole('heading', { name: 'Nightly backup' })
    expect(screen.queryByRole('link', { name: /edit/i })).not.toBeInTheDocument()
  })

  it('shows the edit link for an ADMIN', async () => {
    loginAs('ADMIN')
    const task = seedTask({ name: 'Nightly backup' })

    renderAt(`/tasks/${task.id}`)

    expect(await screen.findByRole('link', { name: /edit/i })).toHaveAttribute(
      'href',
      `/tasks/${task.id}/edit`,
    )
  })

  it('shows a not-found message for an unknown task id', async () => {
    renderAt('/tasks/missing-id')

    expect(await screen.findByText(/not found/i)).toBeInTheDocument()
  })
})
