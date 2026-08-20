import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { listTasks } from '../api/client'
import type { Task } from '../api/types'
import { TaskTable } from '../components/organisms/TaskTable'
import { useAuth } from '../auth/AuthContext'

export function TaskListPage() {
  const { isAdmin } = useAuth()
  const [searchParams, setSearchParams] = useSearchParams()
  const serviceFilter = searchParams.get('service') ?? ''

  const [tasks, setTasks] = useState<Task[] | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false
    setTasks(null)
    listTasks(serviceFilter || undefined)
      .then((result) => {
        if (!cancelled) setTasks(result)
      })
      .catch(() => {
        if (!cancelled) setError('Could not load tasks.')
      })
    return () => {
      cancelled = true
    }
  }, [serviceFilter])

  function handleFilterChange(value: string) {
    if (value) setSearchParams({ service: value })
    else setSearchParams({})
  }

  return (
    <section>
      <header>
        <h1>Tasks</h1>
        {isAdmin && <Link to="/tasks/new">New task</Link>}
      </header>

      <div className="form-field">
        <label htmlFor="service-filter">Filter by service</label>
        <input
          id="service-filter"
          value={serviceFilter}
          onChange={(e) => handleFilterChange(e.target.value)}
        />
      </div>

      {error && <p role="alert">{error}</p>}
      {!error && tasks === null && <p>Loading tasks…</p>}
      {!error && tasks !== null && <TaskTable tasks={tasks} isAdmin={isAdmin} />}
    </section>
  )
}
