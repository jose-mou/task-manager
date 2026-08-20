import { describe, expect, it } from 'vitest'
import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { ServicesAdminPage } from './ServicesAdminPage'
import { seedService } from '../mocks/handlers'
import { AuthProvider } from '../auth/AuthContext'
import { loginAs } from '../test/authHelpers'

function renderAt(path: string) {
  return render(
    <AuthProvider>
      <MemoryRouter initialEntries={[path]}>
        <Routes>
          <Route path="/admin/services" element={<ServicesAdminPage />} />
          <Route path="/login" element={<div>Login page</div>} />
        </Routes>
      </MemoryRouter>
    </AuthProvider>,
  )
}

/** Every key/value a Web Storage area currently holds, as one string. */
function dump(storage: Storage): string {
  const entries: string[] = []
  for (let i = 0; i < storage.length; i += 1) {
    const key = storage.key(i)
    if (key !== null) entries.push(`${key}=${storage.getItem(key)}`)
  }
  return entries.join('\n')
}

describe('ServicesAdminPage', () => {

  it('lists the registered services', async () => {
    seedService({ name: 'backup-service' })
    seedService({ name: 'reporting-service' })
    loginAs('ADMIN')

    renderAt('/admin/services')

    expect(await screen.findByText('backup-service')).toBeInTheDocument()
    expect(screen.getByText('reporting-service')).toBeInTheDocument()
  })

  it('registers a service and shows its one-time credentials', async () => {
    loginAs('ADMIN')
    const user = userEvent.setup()
    renderAt('/admin/services')

    await screen.findByText(/no services registered/i)
    await user.type(screen.getByLabelText(/new service name/i), 'backup-service')
    await user.click(screen.getByRole('button', { name: /register service/i }))

    expect(await screen.findByRole('dialog')).toBeInTheDocument()
    expect(screen.getByText(/cannot be retrieved again/i)).toBeInTheDocument()
    expect(screen.getByText('backup-service')).toBeInTheDocument()
  })

  it('shows the 400 validation message when the service name is blank', async () => {
    loginAs('ADMIN')
    const user = userEvent.setup()
    renderAt('/admin/services')

    await screen.findByText(/no services registered/i)
    await user.click(screen.getByRole('button', { name: /register service/i }))

    expect(await screen.findByText('must not be blank')).toBeInTheDocument()
    expect(screen.getByLabelText(/new service name/i)).toBeInvalid()
  })

  it('closes the credential dialog and never shows it again for that action', async () => {
    loginAs('ADMIN')
    const user = userEvent.setup()
    renderAt('/admin/services')

    await user.type(screen.getByLabelText(/new service name/i), 'backup-service')
    await user.click(screen.getByRole('button', { name: /register service/i }))
    await screen.findByRole('dialog')

    await user.click(screen.getByRole('button', { name: /close/i }))

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
  })

  it('leaves no way to recover the apiSecret once the dialog is closed', async () => {
    loginAs('ADMIN')
    const user = userEvent.setup()
    const { unmount } = renderAt('/admin/services')

    await user.type(screen.getByLabelText(/new service name/i), 'backup-service')
    await user.click(screen.getByRole('button', { name: /register service/i }))

    const dialog = await screen.findByRole('dialog')
    const secret = dialog.querySelectorAll('dd')[1].textContent ?? ''
    expect(secret).toMatch(/^[A-Za-z0-9_-]{43}$/)

    await user.click(screen.getByRole('button', { name: /close/i }))

    // The secret only ever lived in the register response held in component
    // state: it must be in no storage, in no URL, and gone from the document.
    expect(dump(sessionStorage)).not.toContain(secret)
    expect(dump(localStorage)).not.toContain(secret)
    expect(window.location.href).not.toContain(secret)
    expect(document.body.textContent).not.toContain(secret)

    // Reopening the screen must not bring it back either.
    unmount()
    renderAt('/admin/services')
    await screen.findByText('backup-service')
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(document.body.textContent).not.toContain(secret)
  })

  it('renames a service', async () => {
    const service = seedService({ name: 'old-name' })
    loginAs('ADMIN')
    const user = userEvent.setup()
    renderAt('/admin/services')

    await screen.findByText('old-name')
    await user.click(screen.getByRole('button', { name: /rename/i }))
    const input = screen.getByLabelText(`Rename ${service.name}`)
    await user.clear(input)
    await user.type(input, 'new-name')
    await user.click(screen.getByRole('button', { name: /save/i }))

    expect(await screen.findByText('new-name')).toBeInTheDocument()
    expect(screen.queryByText('old-name')).not.toBeInTheDocument()
  })

  it('deletes a service', async () => {
    seedService({ name: 'to-delete' })
    loginAs('ADMIN')
    const user = userEvent.setup()
    renderAt('/admin/services')

    await screen.findByText('to-delete')
    await user.click(screen.getByRole('button', { name: /delete/i }))

    expect(await screen.findByText(/no services registered/i)).toBeInTheDocument()
  })

  it('rotates credentials and shows the fresh one-time pair', async () => {
    seedService({ name: 'backup-service' })
    loginAs('ADMIN')
    const user = userEvent.setup()
    renderAt('/admin/services')

    const row = (await screen.findByText('backup-service')).closest('tr')!
    await user.click(within(row).getByRole('button', { name: /rotate credentials/i }))

    expect(await screen.findByRole('dialog')).toBeInTheDocument()
  })

  it('redirects to login when the session is rejected while loading services', async () => {
    renderAt('/admin/services')

    expect(await screen.findByText('Login page')).toBeInTheDocument()
  })
})
