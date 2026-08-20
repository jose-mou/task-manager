import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { listTasks } from '../api/client'
import type { Task } from '../api/types'
import { FormField } from '../components/molecules/FormField'
import { Input } from '../components/atoms/Input'
import { TaskTable } from '../components/organisms/TaskTable'
import { useAuth } from '../auth/AuthContext'

/**
 * `/tasks` is public, and `GET /api/services` is ADMIN-only, so the service
 * filter cannot be a select fed by the registry: an anonymous visitor would
 * have no way to fill it. It is therefore a free-text field matching the
 * `service` query parameter of `GET /api/tasks`, which the contract defines as
 * an exact service name.
 */
export function TaskListPage() {
  const { isAdmin } = useAuth()
  const [searchParams, setSearchParams] = useSearchParams()
  const serviceFilter = searchParams.get('service') ?? ''
  // The contract's `service` parameter is `minLength: 1, pattern: \S`, so a
  // blank value is sent as no filter at all rather than as an empty query.
  const activeFilter = serviceFilter.trim()

  const [tasks, setTasks] = useState<Task[] | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false
    setTasks(null)
    setError(null)
    listTasks(activeFilter || undefined)
      .then((result) => {
        if (!cancelled) setTasks(result)
      })
      .catch(() => {
        if (!cancelled) setError('Could not load tasks.')
      })
    return () => {
      cancelled = true
    }
  }, [activeFilter])

  function handleFilterChange(value: string) {
    // `replace` keeps the browser history usable: typing a service name would
    // otherwise push one entry per keystroke.
    setSearchParams(value ? { service: value } : {}, { replace: true })
  }

  return (
    <section>
      <header>
        <h1>Tasks</h1>
        {isAdmin && <Link to="/tasks/new">New task</Link>}
      </header>

      <FormField htmlFor="service-filter" label="Filter by service">
        {(control) => (
          <Input
            {...control}
            value={serviceFilter}
            onChange={(e) => handleFilterChange(e.target.value)}
          />
        )}
      </FormField>

      {error && <p role="alert">{error}</p>}
      {!error && tasks === null && <p>Loading tasks…</p>}
      {!error && tasks !== null && (
        <TaskTable
          tasks={tasks}
          isAdmin={isAdmin}
          emptyMessage={
            activeFilter
              ? `No tasks owned by "${activeFilter}".`
              : 'No tasks yet.'
          }
        />
      )}
    </section>
  )
}
