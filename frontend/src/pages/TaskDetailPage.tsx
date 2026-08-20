import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { getTask } from '../api/client'
import { ApiNotFoundError } from '../api/errors'
import type { Task } from '../api/types'
import { useAuth } from '../auth/AuthContext'

export function TaskDetailPage() {
  const { id } = useParams<{ id: string }>()
  const { isAdmin } = useAuth()
  const [task, setTask] = useState<Task | null>(null)
  const [notFound, setNotFound] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!id) return
    let cancelled = false
    getTask(id)
      .then((result) => {
        if (!cancelled) setTask(result)
      })
      .catch((err) => {
        if (cancelled) return
        if (err instanceof ApiNotFoundError) setNotFound(true)
        else setError('Could not load the task.')
      })
    return () => {
      cancelled = true
    }
  }, [id])

  if (notFound) {
    return (
      <section>
        <h1>Task not found</h1>
        <p>No task exists with this id.</p>
      </section>
    )
  }

  if (error) {
    return (
      <section>
        <h1>Task</h1>
        <p role="alert">{error}</p>
      </section>
    )
  }

  if (!task) {
    return (
      <section>
        <h1>Task</h1>
        <p>Loading task…</p>
      </section>
    )
  }

  return (
    <section>
      <header>
        <h1>{task.name}</h1>
        {isAdmin && <Link to={`/tasks/${task.id}/edit`}>Edit</Link>}
      </header>

      <dl>
        <dt>Service</dt>
        <dd>{task.service}</dd>

        <dt>Status</dt>
        <dd>{task.status}</dd>

        <dt>Script</dt>
        <dd>{task.script}</dd>

        <dt>Description</dt>
        <dd>{task.description ?? '—'}</dd>

        <dt>Scheduled</dt>
        <dd>{task.scheduled ? 'Yes' : 'No'}</dd>

        <dt>Cron expression</dt>
        <dd>{task.cronExpr ?? '—'}</dd>

        <dt>Max executions</dt>
        <dd>{task.maxExecutions ?? 'Unlimited'}</dd>

        <dt>Creation date</dt>
        <dd>{task.creationDate}</dd>

        <dt>Modification date</dt>
        <dd>{task.modificationDate}</dd>
      </dl>
    </section>
  )
}
