import { useCallback, useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { createTask, getTask, updateTask } from '../api/client'
import { listServices } from '../api/servicesClient'
import {
  ApiConflictError,
  ApiForbiddenError,
  ApiNotFoundError,
  ApiUnauthorizedError,
  ApiValidationError,
} from '../api/errors'
import type { FieldError, Service, Task, TaskRequest } from '../api/types'
import { TaskForm, type TaskFormValues } from '../components/organisms/TaskForm'
import { useAuth } from '../auth/AuthContext'

function toInitialValues(task: Task): Partial<TaskFormValues> {
  return {
    name: task.name,
    service: task.service,
    description: task.description,
    status: task.status,
    script: task.script,
    cronExpr: task.cronExpr,
    maxExecutions: task.maxExecutions,
    scheduled: task.scheduled,
  }
}

export function TaskFormPage() {
  const { id } = useParams<{ id: string }>()
  const isEdit = id !== undefined
  const navigate = useNavigate()
  const { logout } = useAuth()

  const [services, setServices] = useState<Service[]>()
  const [initialValues, setInitialValues] = useState<Partial<TaskFormValues>>()
  const [notFound, setNotFound] = useState(false)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [submitError, setSubmitError] = useState<string | null>(null)
  const [fieldErrors, setFieldErrors] = useState<FieldError[]>()
  const [submitting, setSubmitting] = useState(false)

  /** A rejected session on a write is logged out and sent back to /login. */
  const handleAuthError = useCallback(
    (error: unknown): boolean => {
      if (error instanceof ApiUnauthorizedError || error instanceof ApiForbiddenError) {
        logout()
        navigate('/login')
        return true
      }
      return false
    },
    [logout, navigate],
  )

  useEffect(() => {
    let cancelled = false
    listServices()
      .then((result) => {
        if (!cancelled) setServices(result)
      })
      .catch((error) => {
        if (cancelled) return
        if (!handleAuthError(error)) setLoadError('Could not load the services.')
      })
    return () => {
      cancelled = true
    }
  }, [handleAuthError])

  useEffect(() => {
    if (!isEdit || !id) return
    let cancelled = false
    getTask(id)
      .then((task) => {
        if (!cancelled) setInitialValues(toInitialValues(task))
      })
      .catch((error) => {
        if (cancelled) return
        if (error instanceof ApiNotFoundError) setNotFound(true)
        else setLoadError('Could not load the task.')
      })
    return () => {
      cancelled = true
    }
  }, [isEdit, id])

  async function handleSubmit(payload: TaskRequest) {
    setSubmitting(true)
    setFieldErrors(undefined)
    setSubmitError(null)
    try {
      if (isEdit && id) {
        await updateTask(id, payload)
      } else {
        await createTask(payload)
      }
      navigate('/tasks')
    } catch (error) {
      if (error instanceof ApiValidationError) {
        setFieldErrors(error.fieldErrors)
      } else if (error instanceof ApiConflictError) {
        setFieldErrors([{ field: 'name', message: error.message }])
      } else if (!handleAuthError(error)) {
        setSubmitError('Could not save the task.')
      }
    } finally {
      setSubmitting(false)
    }
  }

  if (notFound) {
    return (
      <section>
        <h1>Task not found</h1>
        <p>No task exists with this id.</p>
      </section>
    )
  }

  if ((isEdit && !initialValues) || !services) {
    return (
      <section>
        <h1>{isEdit ? 'Edit task' : 'New task'}</h1>
        {loadError ? <p role="alert">{loadError}</p> : <p>Loading…</p>}
      </section>
    )
  }

  return (
    <section>
      <h1>{isEdit ? 'Edit task' : 'New task'}</h1>
      {submitError && <p role="alert">{submitError}</p>}
      <TaskForm
        initialValues={initialValues}
        fieldErrors={fieldErrors}
        submitting={submitting}
        services={services}
        onSubmit={handleSubmit}
      />
    </section>
  )
}
