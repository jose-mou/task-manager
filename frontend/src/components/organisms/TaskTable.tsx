import { Link } from 'react-router-dom'
import type { Task } from '../../api/types'

interface TaskTableProps {
  tasks: Task[]
  /** Edit links are only rendered for an ADMIN; anonymous/USER get read-only rows. */
  isAdmin?: boolean
  /** Shown instead of the table when there is nothing to list. */
  emptyMessage?: string
}

export function TaskTable({
  tasks,
  isAdmin = false,
  emptyMessage = 'No tasks yet.',
}: TaskTableProps) {
  if (tasks.length === 0) {
    return <p>{emptyMessage}</p>
  }

  return (
    <table>
      <thead>
        <tr>
          <th>Name</th>
          <th>Service</th>
          <th>Status</th>
          <th>Scheduled</th>
          <th>Modification date</th>
          {isAdmin && <th aria-label="Actions" />}
        </tr>
      </thead>
      <tbody>
        {tasks.map((task) => (
          <tr key={task.id}>
            <td>
              <Link to={`/tasks/${task.id}`}>{task.name}</Link>
            </td>
            <td>{task.service}</td>
            <td>{task.status}</td>
            <td>{task.scheduled ? 'Yes' : 'No'}</td>
            <td>{task.modificationDate}</td>
            {isAdmin && (
              <td>
                <Link to={`/tasks/${task.id}/edit`}>Edit</Link>
              </td>
            )}
          </tr>
        ))}
      </tbody>
    </table>
  )
}
