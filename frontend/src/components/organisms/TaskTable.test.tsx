import { describe, expect, it } from 'vitest'
import { render, screen, within } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { TaskTable } from './TaskTable'
import type { Task } from '../../api/types'

function task(overrides: Partial<Task>): Task {
  return {
    id: 'id-1',
    name: 'Nightly backup',
    creationDate: '2026-08-19T22:00:00Z',
    modificationDate: '2026-08-19T22:30:00Z',
    service: 'backup-service',
    description: null,
    status: 'RUNNING',
    script: '/opt/scripts/backup.sh',
    cronExpr: '0 0 2 * * *',
    maxExecutions: 30,
    scheduled: true,
    ...overrides,
  }
}

describe('TaskTable', () => {
  it('renders one row per task with name, service, status, scheduled and modificationDate', () => {
    render(
      <MemoryRouter>
        <TaskTable tasks={[task({})]} />
      </MemoryRouter>,
    )

    const row = screen.getByRole('row', { name: /Nightly backup/ })
    expect(within(row).getByText('backup-service')).toBeInTheDocument()
    expect(within(row).getByText('RUNNING')).toBeInTheDocument()
    expect(within(row).getByText('Yes')).toBeInTheDocument()
    expect(within(row).getByText('2026-08-19T22:30:00Z')).toBeInTheDocument()
  })

  it('renders tasks in the given order without re-sorting', () => {
    render(
      <MemoryRouter>
        <TaskTable
          tasks={[
            task({ id: 'a', name: 'Newest task' }),
            task({ id: 'b', name: 'Oldest task' }),
          ]}
        />
      </MemoryRouter>,
    )

    const rows = screen.getAllByRole('row')
    // rows[0] is the header row
    expect(within(rows[1]).getByText('Newest task')).toBeInTheDocument()
    expect(within(rows[2]).getByText('Oldest task')).toBeInTheDocument()
  })

  it('links each row to its edit screen', () => {
    render(
      <MemoryRouter>
        <TaskTable tasks={[task({ id: 'task-42' })]} />
      </MemoryRouter>,
    )

    expect(screen.getByRole('link', { name: /edit/i })).toHaveAttribute(
      'href',
      '/tasks/task-42/edit',
    )
  })

  it('renders an empty state when there are no tasks', () => {
    render(
      <MemoryRouter>
        <TaskTable tasks={[]} />
      </MemoryRouter>,
    )

    expect(screen.getByText(/no tasks/i)).toBeInTheDocument()
  })
})
