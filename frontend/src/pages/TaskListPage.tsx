import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { listTasks } from '../api/client'
import type { Task } from '../api/types'
import { TaskTable } from '../components/organisms/TaskTable'

export function TaskListPage() {
  const [tasks, setTasks] = useState<Task[] | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false
    listTasks()
      .then((result) => {
        if (!cancelled) setTasks(result)
      })
      .catch(() => {
        if (!cancelled) setError('Could not load tasks.')
      })
    return () => {
      cancelled = true
    }
  }, [])

  return (
    <section>
      <header>
        <h1>Tasks</h1>
        <Link to="/tasks/new">New task</Link>
      </header>

      {error && <p role="alert">{error}</p>}
      {!error && tasks === null && <p>Loading tasks…</p>}
      {!error && tasks !== null && <TaskTable tasks={tasks} />}
    </section>
  )
}
