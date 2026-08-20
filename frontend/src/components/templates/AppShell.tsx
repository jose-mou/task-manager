import { Link, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../../auth/AuthContext'

export function AppShell() {
  const { session, isAdmin, logout } = useAuth()
  const navigate = useNavigate()

  function handleLogout() {
    logout()
    navigate('/tasks')
  }

  return (
    <div className="app-shell">
      <nav>
        <Link to="/tasks">Tasks</Link>
        {isAdmin && <Link to="/admin/services">Services</Link>}
        {session ? (
          <>
            <Link to="/account/password">Change password</Link>
            <button type="button" onClick={handleLogout}>
              Log out
            </button>
          </>
        ) : (
          <Link to="/login">Log in</Link>
        )}
      </nav>
      <main>
        <Outlet />
      </main>
    </div>
  )
}
