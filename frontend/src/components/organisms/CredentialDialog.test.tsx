import { describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { CredentialDialog } from './CredentialDialog'
import type { ServiceCredentials } from '../../api/types'

const CREDENTIALS: ServiceCredentials = {
  id: 'svc-1',
  name: 'backup-service',
  apiKey: 'key-value-123',
  apiSecret: 'secret-value-456',
}

describe('CredentialDialog', () => {
  it('shows the one-time apiKey and apiSecret with a never-again warning', () => {
    render(<CredentialDialog credentials={CREDENTIALS} onClose={vi.fn()} />)

    expect(screen.getByText('key-value-123')).toBeInTheDocument()
    expect(screen.getByText('secret-value-456')).toBeInTheDocument()
    expect(screen.getByText(/cannot be retrieved again/i)).toBeInTheDocument()
  })

  it('copies both credential values when the copy action is used', async () => {
    // userEvent.setup() installs its own clipboard stub, so the spy is
    // attached after setup instead of replacing navigator.clipboard outright.
    const user = userEvent.setup()
    const writeText = vi.spyOn(navigator.clipboard, 'writeText').mockResolvedValue(undefined)
    render(<CredentialDialog credentials={CREDENTIALS} onClose={vi.fn()} />)

    await user.click(screen.getByRole('button', { name: /copy/i }))

    expect(writeText).toHaveBeenCalledWith(expect.stringContaining('key-value-123'))
    expect(writeText).toHaveBeenCalledWith(expect.stringContaining('secret-value-456'))
    expect(await screen.findByText(/copied to clipboard/i)).toBeInTheDocument()
  })

  it('calls onClose when the dialog is closed', async () => {
    const user = userEvent.setup()
    const onClose = vi.fn()
    render(<CredentialDialog credentials={CREDENTIALS} onClose={onClose} />)

    await user.click(screen.getByRole('button', { name: /close/i }))

    expect(onClose).toHaveBeenCalled()
  })

  it('moves focus into the dialog when it opens', () => {
    render(<CredentialDialog credentials={CREDENTIALS} onClose={vi.fn()} />)

    expect(screen.getByRole('dialog')).toHaveFocus()
  })

  it('closes on Escape', async () => {
    const user = userEvent.setup()
    const onClose = vi.fn()
    render(<CredentialDialog credentials={CREDENTIALS} onClose={onClose} />)

    await user.keyboard('{Escape}')

    expect(onClose).toHaveBeenCalled()
  })

  it('keeps keyboard focus inside the dialog', async () => {
    const user = userEvent.setup()
    render(<CredentialDialog credentials={CREDENTIALS} onClose={vi.fn()} />)

    await user.tab()
    expect(screen.getByRole('button', { name: /copy/i })).toHaveFocus()

    await user.tab()
    expect(screen.getByRole('button', { name: /close/i })).toHaveFocus()

    await user.tab()
    expect(screen.getByRole('button', { name: /copy/i })).toHaveFocus()
  })
})
