import { Navigate, Route, Routes } from 'react-router-dom'
import { AppShell } from '../components/templates/AppShell'
import { TaskListPage } from '../pages/TaskListPage'
import { TaskDetailPage } from '../pages/TaskDetailPage'
import { TaskFormPage } from '../pages/TaskFormPage'
import { LoginPage } from '../pages/LoginPage'
import { ChangePasswordPage } from '../pages/ChangePasswordPage'
import { ServicesAdminPage } from '../pages/ServicesAdminPage'
import { RequireAuth } from '../auth/RequireAuth'
import { RequireAdmin } from '../auth/RequireAdmin'

export function AppRoutes() {
  return (
    <Routes>
      <Route element={<AppShell />}>
        <Route path="/" element={<Navigate to="/tasks" replace />} />
        <Route path="/tasks" element={<TaskListPage />} />
        <Route path="/login" element={<LoginPage />} />

        <Route element={<RequireAuth />}>
          <Route path="/account/password" element={<ChangePasswordPage />} />
        </Route>

        <Route element={<RequireAdmin />}>
          <Route path="/tasks/new" element={<TaskFormPage />} />
          <Route path="/tasks/:id/edit" element={<TaskFormPage />} />
          <Route path="/admin/services" element={<ServicesAdminPage />} />
        </Route>

        {/* Kept last: /tasks/:id must not shadow the static routes above. */}
        <Route path="/tasks/:id" element={<TaskDetailPage />} />
      </Route>
    </Routes>
  )
}
