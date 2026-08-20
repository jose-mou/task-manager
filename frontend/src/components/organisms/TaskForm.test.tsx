import { describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { TaskForm } from './TaskForm'
import type { Service } from '../../api/types'

const SERVICES: Service[] = [
  {
    id: 'svc-1',
    name: 'backup-service',
    creationDate: '2026-08-18T09:00:00Z',
    modificationDate: '2026-08-18T09:00:00Z',
  },
  {
    id: 'svc-2',
    name: 'reporting-service',
    creationDate: '2026-08-19T08:30:00Z',
    modificationDate: '2026-08-19T08:30:00Z',
  },
]

describe('TaskForm', () => {
  it('renders every editable field', () => {
    render(<TaskForm services={SERVICES} onSubmit={vi.fn()} />)

    expect(screen.getByLabelText(/name/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/service/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/description/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/^script/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/scheduled/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/cron/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/max executions/i)).toBeInTheDocument()

    const statusSelect = screen.getByLabelText(/status/i) as HTMLSelectElement
    const options = Array.from(statusSelect.options).map((o) => o.value)
    expect(options).toEqual(['CREATED', 'RUNNING', 'COMPLETED', 'CANCELED'])
  })

  it('feeds the service select from the given registered services', () => {
    render(<TaskForm services={SERVICES} onSubmit={vi.fn()} />)

    const serviceSelect = screen.getByLabelText(/service/i) as HTMLSelectElement
    const options = Array.from(serviceSelect.options).map((o) => o.value)
    expect(options).toEqual(['', 'backup-service', 'reporting-service'])
  })

  it('disables cronExpr and maxExecutions until scheduled is checked', async () => {
    const user = userEvent.setup()
    render(<TaskForm services={SERVICES} onSubmit={vi.fn()} />)

    expect(screen.getByLabelText(/cron/i)).toBeDisabled()
    expect(screen.getByLabelText(/max executions/i)).toBeDisabled()

    await user.click(screen.getByLabelText(/scheduled/i))

    expect(screen.getByLabelText(/cron/i)).toBeEnabled()
    expect(screen.getByLabelText(/max executions/i)).toBeEnabled()
  })

  it('submits the filled-in fields as a TaskRequest payload', async () => {
    const user = userEvent.setup()
    const onSubmit = vi.fn()
    render(<TaskForm services={SERVICES} onSubmit={onSubmit} />)

    await user.type(screen.getByLabelText(/name/i), 'Report generation')
    await user.selectOptions(screen.getByLabelText(/service/i), 'reporting-service')
    await user.type(screen.getByLabelText(/^script/i), '/opt/scripts/report.sh')
    await user.click(screen.getByRole('button', { name: /save/i }))

    expect(onSubmit).toHaveBeenCalledWith(
      expect.objectContaining({
        name: 'Report generation',
        service: 'reporting-service',
        script: '/opt/scripts/report.sh',
        status: 'CREATED',
        scheduled: false,
      }),
    )
  })

  it('includes cronExpr and maxExecutions when scheduled is checked', async () => {
    const user = userEvent.setup()
    const onSubmit = vi.fn()
    render(<TaskForm services={SERVICES} onSubmit={onSubmit} />)

    await user.type(screen.getByLabelText(/name/i), 'Nightly backup')
    await user.selectOptions(screen.getByLabelText(/service/i), 'backup-service')
    await user.type(screen.getByLabelText(/^script/i), '/opt/scripts/backup.sh')
    await user.click(screen.getByLabelText(/scheduled/i))
    await user.type(screen.getByLabelText(/cron/i), '0 0 2 * * *')
    await user.type(screen.getByLabelText(/max executions/i), '30')
    await user.click(screen.getByRole('button', { name: /save/i }))

    expect(onSubmit).toHaveBeenCalledWith(
      expect.objectContaining({
        cronExpr: '0 0 2 * * *',
        maxExecutions: 30,
        scheduled: true,
      }),
    )
  })

  it('pre-fills fields from initialValues for editing', () => {
    render(
      <TaskForm
        services={SERVICES}
        onSubmit={vi.fn()}
        initialValues={{
          name: 'Existing task',
          service: 'backup-service',
          script: '/opt/scripts/existing.sh',
          status: 'COMPLETED',
          scheduled: true,
          cronExpr: '0 0 2 * * *',
          maxExecutions: 5,
        }}
      />,
    )

    expect(screen.getByLabelText(/name/i)).toHaveValue('Existing task')
    expect(screen.getByLabelText(/service/i)).toHaveValue('backup-service')
    expect(screen.getByLabelText(/status/i)).toHaveValue('COMPLETED')
    expect(screen.getByLabelText(/scheduled/i)).toBeChecked()
    expect(screen.getByLabelText(/cron/i)).toHaveValue('0 0 2 * * *')
    expect(screen.getByLabelText(/max executions/i)).toHaveValue(5)
  })

  it('shows inline field errors mapped from the API response', () => {
    render(
      <TaskForm
        services={SERVICES}
        onSubmit={vi.fn()}
        fieldErrors={[
          { field: 'name', message: 'must not be blank' },
          { field: 'maxExecutions', message: 'must be greater than or equal to 1' },
        ]}
      />,
    )

    expect(screen.getByText('must not be blank')).toBeInTheDocument()
    expect(
      screen.getByText('must be greater than or equal to 1'),
    ).toBeInTheDocument()
  })

  it('associates every field error with its own control', () => {
    render(
      <TaskForm
        services={SERVICES}
        onSubmit={vi.fn()}
        fieldErrors={[
          { field: 'description', message: 'description is invalid' },
          { field: 'status', message: 'must be one of CREATED, RUNNING, COMPLETED, CANCELED' },
        ]}
      />,
    )

    expect(screen.getByLabelText(/description/i)).toHaveAccessibleDescription(
      'description is invalid',
    )
    expect(screen.getByLabelText(/status/i)).toHaveAccessibleDescription(
      'must be one of CREATED, RUNNING, COMPLETED, CANCELED',
    )
    expect(screen.getByLabelText(/description/i)).toBeInvalid()
  })

  it('round-trips the scheduling fields of an unscheduled task instead of wiping them', async () => {
    const user = userEvent.setup()
    const onSubmit = vi.fn()
    render(
      <TaskForm
        services={SERVICES}
        onSubmit={onSubmit}
        initialValues={{
          name: 'Cache warmup',
          service: 'backup-service',
          script: '/opt/scripts/warmup.sh',
          scheduled: false,
          cronExpr: '0 0 2 * * *',
          maxExecutions: 1,
        }}
      />,
    )

    await user.click(screen.getByRole('button', { name: /save/i }))

    expect(onSubmit).toHaveBeenCalledWith(
      expect.objectContaining({
        scheduled: false,
        cronExpr: '0 0 2 * * *',
        maxExecutions: 1,
      }),
    )
  })

  it('surfaces the 409 duplicate-name error on the name field', () => {
    render(
      <TaskForm
        services={SERVICES}
        onSubmit={vi.fn()}
        fieldErrors={[
          { field: 'name', message: "A task with name 'backup' already exists for service 'backup-service'" },
        ]}
      />,
    )

    const nameInput = screen.getByLabelText(/name/i)
    expect(nameInput).toHaveAccessibleDescription(
      "A task with name 'backup' already exists for service 'backup-service'",
    )
  })
})
