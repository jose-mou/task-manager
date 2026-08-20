import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from './AuthContext'

/** Route guard: any logged-in user (ADMIN or USER). Anonymous visitors go to /login. */
export function RequireAuth() {
  const { session } = useAuth()
  const location = useLocation()

  if (!session) {
    return <Navigate to="/login" replace state={{ from: location }} />
  }
  return <Outlet />
}
