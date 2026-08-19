import { useState, type FormEvent } from 'react'
import type { FieldError, TaskRequest, TaskStatus } from '../../api/types'
import { TASK_STATUSES } from '../../api/types'
import { FormField } from '../molecules/FormField'
import { fieldErrorProps } from '../../lib/fieldErrorProps'
import { Input } from '../atoms/Input'
import { Select } from '../atoms/Select'
import { Checkbox } from '../atoms/Checkbox'

export interface TaskFormValues {
  name: string
  service: string
  description: string | null
  status: TaskStatus
  script: string
  cronExpr: string | null
  maxExecutions: number | null
  scheduled: boolean
}

const DEFAULT_VALUES: TaskFormValues = {
  name: '',
  service: '',
  description: null,
  status: 'CREATED',
  script: '',
  cronExpr: null,
  maxExecutions: null,
  scheduled: false,
}

interface TaskFormProps {
  initialValues?: Partial<TaskFormValues>
  fieldErrors?: FieldError[]
  submitting?: boolean
  submitLabel?: string
  onSubmit: (payload: TaskRequest) => void
}

function errorFor(fieldErrors: FieldError[] | undefined, field: string) {
  return fieldErrors?.find((e) => e.field === field)?.message
}

export function TaskForm({
  initialValues,
  fieldErrors,
  submitting = false,
  submitLabel = 'Save',
  onSubmit,
}: TaskFormProps) {
  const [values, setValues] = useState<TaskFormValues>({
    ...DEFAULT_VALUES,
    ...initialValues,
  })

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const payload: TaskRequest = {
      name: values.name,
      service: values.service,
      description: values.description,
      status: values.status,
      script: values.script,
      cronExpr: values.scheduled ? values.cronExpr : null,
      maxExecutions: values.maxExecutions,
      scheduled: values.scheduled,
    }
    onSubmit(payload)
  }

  return (
    <form onSubmit={handleSubmit} noValidate>
      <FormField htmlFor="name" label="Name" error={errorFor(fieldErrors, 'name')}>
        <Input
          id="name"
          value={values.name}
          onChange={(e) => setValues((v) => ({ ...v, name: e.target.value }))}
          {...fieldErrorProps('name', errorFor(fieldErrors, 'name'))}
        />
      </FormField>

      <FormField
        htmlFor="service"
        label="Service"
        error={errorFor(fieldErrors, 'service')}
      >
        <Input
          id="service"
          value={values.service}
          onChange={(e) => setValues((v) => ({ ...v, service: e.target.value }))}
          {...fieldErrorProps('service', errorFor(fieldErrors, 'service'))}
        />
      </FormField>

      <FormField
        htmlFor="description"
        label="Description"
        error={errorFor(fieldErrors, 'description')}
      >
        <Input
          id="description"
          value={values.description ?? ''}
          onChange={(e) =>
            setValues((v) => ({ ...v, description: e.target.value || null }))
          }
        />
      </FormField>

      <FormField htmlFor="status" label="Status" error={errorFor(fieldErrors, 'status')}>
        <Select
          id="status"
          value={values.status}
          onChange={(e) =>
            setValues((v) => ({ ...v, status: e.target.value as TaskStatus }))
          }
        >
          {TASK_STATUSES.map((status) => (
            <option key={status} value={status}>
              {status}
            </option>
          ))}
        </Select>
      </FormField>

      <FormField htmlFor="script" label="Script" error={errorFor(fieldErrors, 'script')}>
        <Input
          id="script"
          value={values.script}
          onChange={(e) => setValues((v) => ({ ...v, script: e.target.value }))}
          {...fieldErrorProps('script', errorFor(fieldErrors, 'script'))}
        />
      </FormField>

      <FormField htmlFor="scheduled" label="Scheduled">
        <Checkbox
          id="scheduled"
          checked={values.scheduled}
          onChange={(e) =>
            setValues((v) => ({ ...v, scheduled: e.target.checked }))
          }
        />
      </FormField>

      <FormField
        htmlFor="cronExpr"
        label="Cron expression"
        error={errorFor(fieldErrors, 'cronExpr')}
      >
        <Input
          id="cronExpr"
          value={values.cronExpr ?? ''}
          disabled={!values.scheduled}
          onChange={(e) =>
            setValues((v) => ({ ...v, cronExpr: e.target.value || null }))
          }
          {...fieldErrorProps('cronExpr', errorFor(fieldErrors, 'cronExpr'))}
        />
      </FormField>

      <FormField
        htmlFor="maxExecutions"
        label="Max executions"
        error={errorFor(fieldErrors, 'maxExecutions')}
      >
        <Input
          id="maxExecutions"
          type="number"
          min={1}
          value={values.maxExecutions ?? ''}
          disabled={!values.scheduled}
          onChange={(e) =>
            setValues((v) => ({
              ...v,
              maxExecutions: e.target.value === '' ? null : Number(e.target.value),
            }))
          }
          {...fieldErrorProps('maxExecutions', errorFor(fieldErrors, 'maxExecutions'))}
        />
      </FormField>

      <button type="submit" disabled={submitting}>
        {submitLabel}
      </button>
    </form>
  )
}
