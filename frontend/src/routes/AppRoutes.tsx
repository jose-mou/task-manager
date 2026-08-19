import { Navigate, Route, Routes } from 'react-router-dom'
import { AppShell } from '../components/templates/AppShell'
import { TaskListPage } from '../pages/TaskListPage'
import { TaskFormPage } from '../pages/TaskFormPage'

export function AppRoutes() {
  return (
    <Routes>
      <Route element={<AppShell />}>
        <Route path="/" element={<Navigate to="/tasks" replace />} />
        <Route path="/tasks" element={<TaskListPage />} />
        <Route path="/tasks/new" element={<TaskFormPage />} />
        <Route path="/tasks/:id/edit" element={<TaskFormPage />} />
      </Route>
    </Routes>
  )
}
