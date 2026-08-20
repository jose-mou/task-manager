import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { AppRoutes } from './AppRoutes'
import { AuthProvider } from '../auth/AuthContext'
import { loginAs } from '../test/authHelpers'

function renderAt(path: string) {
  return render(
    <AuthProvider>
      <MemoryRouter initialEntries={[path]}>
        <AppRoutes />
      </MemoryRouter>
    </AuthProvider>,
  )
}

describe('AppRoutes', () => {
  it('shows a navigation entry linking to the task list', async () => {
    renderAt('/tasks')

    expect(
      await screen.findByRole('link', { name: /^tasks$/i }),
    ).toHaveAttribute('href', '/tasks')
  })

  it('redirects the root path to the task list', async () => {
    renderAt('/')

    expect(await screen.findByRole('heading', { name: /tasks/i })).toBeInTheDocument()
  })

  it('renders the create form at /tasks/new for an ADMIN', async () => {
    loginAs('ADMIN')
    renderAt('/tasks/new')

    expect(
      await screen.findByRole('heading', { name: /new task/i }),
    ).toBeInTheDocument()
  })

  it('redirects an anonymous visitor away from /tasks/new to the login screen', async () => {
    renderAt('/tasks/new')

    expect(await screen.findByRole('heading', { name: /log in/i })).toBeInTheDocument()
  })

  it('redirects a USER-role visitor away from /tasks/new to the task list', async () => {
    loginAs('USER')
    renderAt('/tasks/new')

    expect(await screen.findByRole('heading', { name: /^tasks$/i })).toBeInTheDocument()
  })

  it('redirects an anonymous visitor away from /admin/services to the login screen', async () => {
    renderAt('/admin/services')

    expect(await screen.findByRole('heading', { name: /log in/i })).toBeInTheDocument()
  })

  it('redirects an anonymous visitor away from /account/password to the login screen', async () => {
    renderAt('/account/password')

    expect(await screen.findByRole('heading', { name: /log in/i })).toBeInTheDocument()
  })

  it('shows the admin services navigation link only for an ADMIN', async () => {
    renderAt('/tasks')
    expect(screen.queryByRole('link', { name: /services/i })).not.toBeInTheDocument()
  })
})
