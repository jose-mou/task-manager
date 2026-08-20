import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { AppRoutes } from './AppRoutes'
import { AuthProvider } from '../auth/AuthContext'
import { getSession } from '../auth/session'
import { seedTask } from '../mocks/handlers'
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

  it('redirects an anonymous visitor away from /tasks/:id/edit to the login screen', async () => {
    renderAt('/tasks/some-id/edit')

    expect(await screen.findByRole('heading', { name: /log in/i })).toBeInTheDocument()
  })

  it('redirects a USER-role visitor away from /tasks/:id/edit to the task list', async () => {
    loginAs('USER')
    renderAt('/tasks/some-id/edit')

    expect(await screen.findByRole('heading', { name: /^tasks$/i })).toBeInTheDocument()
  })

  it('redirects an anonymous visitor away from /admin/services to the login screen', async () => {
    renderAt('/admin/services')

    expect(await screen.findByRole('heading', { name: /log in/i })).toBeInTheDocument()
  })

  it('redirects a USER-role visitor away from /admin/services to the task list', async () => {
    loginAs('USER')
    renderAt('/admin/services')

    expect(await screen.findByRole('heading', { name: /^tasks$/i })).toBeInTheDocument()
  })

  it('redirects an anonymous visitor away from /account/password to the login screen', async () => {
    renderAt('/account/password')

    expect(await screen.findByRole('heading', { name: /log in/i })).toBeInTheDocument()
  })

  it('keeps /tasks and /tasks/:id readable for an anonymous visitor', async () => {
    const task = seedTask({ name: 'Nightly backup' })

    const { unmount } = renderAt('/tasks')
    expect(await screen.findByText('Nightly backup')).toBeInTheDocument()
    unmount()

    renderAt(`/tasks/${task.id}`)
    expect(
      await screen.findByRole('heading', { name: 'Nightly backup' }),
    ).toBeInTheDocument()
  })

  it('hides the admin services navigation link from an anonymous visitor', async () => {
    renderAt('/tasks')

    expect(screen.queryByRole('link', { name: /services/i })).not.toBeInTheDocument()
  })

  it('hides the admin services navigation link from a USER', async () => {
    loginAs('USER')
    renderAt('/tasks')

    expect(await screen.findByRole('link', { name: /^tasks$/i })).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: /services/i })).not.toBeInTheDocument()
  })

  it('shows the admin services navigation link to an ADMIN', async () => {
    loginAs('ADMIN')
    renderAt('/tasks')

    expect(await screen.findByRole('link', { name: /services/i })).toHaveAttribute(
      'href',
      '/admin/services',
    )
  })

  it('sends a logged-in ADMIN back to the screen they were bounced from', async () => {
    const user = userEvent.setup()
    renderAt('/admin/services')

    await screen.findByRole('heading', { name: /log in/i })
    await user.type(screen.getByLabelText(/username/i), 'admin')
    await user.type(screen.getByLabelText(/password/i), 'admin')
    await user.click(screen.getByRole('button', { name: /log in/i }))

    expect(
      await screen.findByRole('heading', { name: /^services$/i }),
    ).toBeInTheDocument()
  })

  it('clears the session and the admin navigation on logout', async () => {
    loginAs('ADMIN')
    const user = userEvent.setup()
    renderAt('/tasks')

    await user.click(await screen.findByRole('button', { name: /log out/i }))

    expect(await screen.findByRole('link', { name: /log in/i })).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: /services/i })).not.toBeInTheDocument()
    expect(getSession()).toBeNull()
  })

  it('offers the password change to a logged-in user only', async () => {
    const { unmount } = renderAt('/tasks')
    expect(
      screen.queryByRole('link', { name: /change password/i }),
    ).not.toBeInTheDocument()
    unmount()

    loginAs('USER')
    renderAt('/tasks')
    expect(
      await screen.findByRole('link', { name: /change password/i }),
    ).toHaveAttribute('href', '/account/password')
  })
})
