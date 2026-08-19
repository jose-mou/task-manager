import { useState, type FormEvent } from 'react'
import type { FieldError, TaskRequest, TaskStatus } from '../../api/types'
import { TASK_STATUSES } from '../../api/types'
import { FormField } from '../molecules/FormField'
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
  onSubmit: (payload: TaskRequest) => void
}

export function TaskForm({
  initialValues,
  fieldErrors,
  submitting = false,
  onSubmit,
}: TaskFormProps) {
  const [values, setValues] = useState<TaskFormValues>({
    ...DEFAULT_VALUES,
    ...initialValues,
  })

  const errorFor = (field: string) =>
    fieldErrors?.find((error) => error.field === field)?.message

  function update(patch: Partial<TaskFormValues>) {
    setValues((current) => ({ ...current, ...patch }))
  }

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    // PUT is a full replacement, so every value the form holds is sent back as
    // is: fields disabled by `scheduled` are still round-tripped instead of
    // being silently wiped.
    const payload: TaskRequest = {
      name: values.name,
      service: values.service,
      description: values.description,
      status: values.status,
      script: values.script,
      cronExpr: values.cronExpr,
      maxExecutions: values.maxExecutions,
      scheduled: values.scheduled,
    }
    onSubmit(payload)
  }

  return (
    <form onSubmit={handleSubmit} noValidate>
      <FormField htmlFor="name" label="Name" error={errorFor('name')}>
        {(control) => (
          <Input
            {...control}
            required
            value={values.name}
            onChange={(e) => update({ name: e.target.value })}
          />
        )}
      </FormField>

      <FormField htmlFor="service" label="Service" error={errorFor('service')}>
        {(control) => (
          <Input
            {...control}
            required
            value={values.service}
            onChange={(e) => update({ service: e.target.value })}
          />
        )}
      </FormField>

      <FormField
        htmlFor="description"
        label="Description"
        error={errorFor('description')}
      >
        {(control) => (
          <Input
            {...control}
            value={values.description ?? ''}
            onChange={(e) => update({ description: e.target.value || null })}
          />
        )}
      </FormField>

      <FormField htmlFor="status" label="Status" error={errorFor('status')}>
        {(control) => (
          <Select
            {...control}
            value={values.status}
            onChange={(e) => update({ status: e.target.value as TaskStatus })}
          >
            {TASK_STATUSES.map((status) => (
              <option key={status} value={status}>
                {status}
              </option>
            ))}
          </Select>
        )}
      </FormField>

      <FormField htmlFor="script" label="Script" error={errorFor('script')}>
        {(control) => (
          <Input
            {...control}
            required
            value={values.script}
            onChange={(e) => update({ script: e.target.value })}
          />
        )}
      </FormField>

      <FormField htmlFor="scheduled" label="Scheduled">
        {(control) => (
          <Checkbox
            {...control}
            checked={values.scheduled}
            onChange={(e) => update({ scheduled: e.target.checked })}
          />
        )}
      </FormField>

      <FormField
        htmlFor="cronExpr"
        label="Cron expression"
        error={errorFor('cronExpr')}
      >
        {(control) => (
          <Input
            {...control}
            disabled={!values.scheduled}
            value={values.cronExpr ?? ''}
            onChange={(e) => update({ cronExpr: e.target.value || null })}
          />
        )}
      </FormField>

      <FormField
        htmlFor="maxExecutions"
        label="Max executions"
        error={errorFor('maxExecutions')}
      >
        {(control) => (
          <Input
            {...control}
            type="number"
            min={1}
            step={1}
            disabled={!values.scheduled}
            value={values.maxExecutions ?? ''}
            onChange={(e) =>
              update({
                maxExecutions:
                  e.target.value === '' ? null : Number(e.target.value),
              })
            }
          />
        )}
      </FormField>

      <button type="submit" disabled={submitting}>
        Save
      </button>
    </form>
  )
}
