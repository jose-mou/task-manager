import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { LoginPage } from './LoginPage'
import { AuthProvider } from '../auth/AuthContext'
import { resetUsers } from '../mocks/handlers'

function renderAt(path: string) {
  return render(
    <AuthProvider>
      <MemoryRouter initialEntries={[path]}>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/tasks" element={<div>Task list page</div>} />
        </Routes>
      </MemoryRouter>
    </AuthProvider>,
  )
}

describe('LoginPage', () => {
  it('logs in with the seeded admin and redirects to the task list', async () => {
    const user = userEvent.setup()
    renderAt('/login')

    await user.type(screen.getByLabelText(/username/i), 'admin')
    await user.type(screen.getByLabelText(/password/i), 'admin')
    await user.click(screen.getByRole('button', { name: /log in/i }))

    expect(await screen.findByText('Task list page')).toBeInTheDocument()
  })

  it('shows an error for wrong credentials and stays on the login screen', async () => {
    const user = userEvent.setup()
    renderAt('/login')

    await user.type(screen.getByLabelText(/username/i), 'admin')
    await user.type(screen.getByLabelText(/password/i), 'wrong-password')
    await user.click(screen.getByRole('button', { name: /log in/i }))

    expect(
      await screen.findByText(/invalid username or password/i),
    ).toBeInTheDocument()
  })

  it('rejects an unknown username the same way as a wrong password', async () => {
    resetUsers([])
    const user = userEvent.setup()
    renderAt('/login')

    await user.type(screen.getByLabelText(/username/i), 'ghost')
    await user.type(screen.getByLabelText(/password/i), 'whatever')
    await user.click(screen.getByRole('button', { name: /log in/i }))

    expect(
      await screen.findByText(/invalid username or password/i),
    ).toBeInTheDocument()
  })
})
