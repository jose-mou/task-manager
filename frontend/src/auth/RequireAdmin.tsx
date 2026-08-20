import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from './AuthContext'

/** Route guard: ADMIN only. Anonymous visitors go to /login, USER-role to /tasks. */
export function RequireAdmin() {
  const { session, isAdmin } = useAuth()
  const location = useLocation()

  if (!session) {
    return <Navigate to="/login" replace state={{ from: location }} />
  }
  if (!isAdmin) {
    return <Navigate to="/tasks" replace />
  }
  return <Outlet />
}
