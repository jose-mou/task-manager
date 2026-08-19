import { Link, Outlet } from 'react-router-dom'

export function AppShell() {
  return (
    <div className="app-shell">
      <nav>
        <Link to="/tasks">Tasks</Link>
      </nav>
      <main>
        <Outlet />
      </main>
    </div>
  )
}
