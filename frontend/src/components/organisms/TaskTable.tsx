import { Link } from 'react-router-dom'
import type { Task } from '../../api/types'

interface TaskTableProps {
  tasks: Task[]
}

export function TaskTable({ tasks }: TaskTableProps) {
  if (tasks.length === 0) {
    return <p>No tasks yet.</p>
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
          <th aria-label="Actions" />
        </tr>
      </thead>
      <tbody>
        {tasks.map((task) => (
          <tr key={task.id}>
            <td>{task.name}</td>
            <td>{task.service}</td>
            <td>{task.status}</td>
            <td>{task.scheduled ? 'Yes' : 'No'}</td>
            <td>{task.modificationDate}</td>
            <td>
              <Link to={`/tasks/${task.id}/edit`}>Edit</Link>
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}
