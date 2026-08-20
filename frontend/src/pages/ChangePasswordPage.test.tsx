import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { ChangePasswordPage } from './ChangePasswordPage'
import { AuthProvider } from '../auth/AuthContext'
import { getSession, setSession } from '../auth/session'
import { loginAs } from '../test/authHelpers'

function renderAt(path: string) {
  return render(
    <AuthProvider>
      <MemoryRouter initialEntries={[path]}>
        <Routes>
          <Route path="/account/password" element={<ChangePasswordPage />} />
          <Route path="/login" element={<div>Login page</div>} />
        </Routes>
      </MemoryRouter>
    </AuthProvider>,
  )
}

describe('ChangePasswordPage', () => {
  it('changes the password and shows a success message', async () => {
    loginAs('ADMIN', 'admin')
    const user = userEvent.setup()
    renderAt('/account/password')

    await user.type(screen.getByLabelText(/current password/i), 'admin')
    await user.type(screen.getByLabelText(/new password/i), 'sup3r-s3cret-pass')
    await user.click(screen.getByRole('button', { name: /change password/i }))

    expect(
      await screen.findByText(/password changed successfully/i),
    ).toBeInTheDocument()
  })

  it('shows a 400 error naming currentPassword when it does not match', async () => {
    loginAs('ADMIN', 'admin')
    const user = userEvent.setup()
    renderAt('/account/password')

    await user.type(screen.getByLabelText(/current password/i), 'wrong')
    await user.type(screen.getByLabelText(/new password/i), 'sup3r-s3cret-pass')
    await user.click(screen.getByRole('button', { name: /change password/i }))

    expect(
      await screen.findByText('does not match the current password'),
    ).toBeInTheDocument()
  })

  it('shows a 400 error naming newPassword when it is too short', async () => {
    loginAs('ADMIN', 'admin')
    const user = userEvent.setup()
    renderAt('/account/password')

    await user.type(screen.getByLabelText(/current password/i), 'admin')
    await user.type(screen.getByLabelText(/new password/i), 'short')
    await user.click(screen.getByRole('button', { name: /change password/i }))

    expect(
      await screen.findByText('must be at least 8 characters long'),
    ).toBeInTheDocument()
  })

  it('redirects to login when the session is no longer valid', async () => {
    // No session stored: the mock server rejects with 401.
    const user = userEvent.setup()
    renderAt('/account/password')

    await user.type(screen.getByLabelText(/current password/i), 'admin')
    await user.type(screen.getByLabelText(/new password/i), 'sup3r-s3cret-pass')
    await user.click(screen.getByRole('button', { name: /change password/i }))

    expect(await screen.findByText('Login page')).toBeInTheDocument()
  })

  it('clears the stored session when the API rejects the token', async () => {
    // A stored but no longer accepted token: the API answers 401 and the UI
    // must drop it instead of keeping a session that authenticates nothing.
    setSession({
      token: 'no-longer-accepted',
      expiresAt: new Date(Date.now() + 8 * 60 * 60 * 1000).toISOString(),
      role: 'ADMIN',
    })
    const user = userEvent.setup()
    renderAt('/account/password')

    await user.type(screen.getByLabelText(/current password/i), 'admin')
    await user.type(screen.getByLabelText(/new password/i), 'sup3r-s3cret-pass')
    await user.click(screen.getByRole('button', { name: /change password/i }))

    expect(await screen.findByText('Login page')).toBeInTheDocument()
    expect(getSession()).toBeNull()
  })
})
