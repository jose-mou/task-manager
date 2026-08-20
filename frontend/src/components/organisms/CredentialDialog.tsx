import { useEffect, useRef, useState, type KeyboardEvent } from 'react'
import type { ServiceCredentials } from '../../api/types'

interface CredentialDialogProps {
  credentials: ServiceCredentials
  onClose: () => void
}

const FOCUSABLE = 'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])'

/**
 * One-time credential dialog: the caller renders this only while it holds a
 * freshly received `ServiceCredentials` response in local state. There is no
 * way to reopen it from stored data — once the response is gone, so is the
 * secret, exactly as the API never returns it again.
 */
export function CredentialDialog({ credentials, onClose }: CredentialDialogProps) {
  const dialogRef = useRef<HTMLDivElement>(null)
  const [copied, setCopied] = useState(false)

  useEffect(() => {
    dialogRef.current?.focus()
  }, [])

  async function handleCopy() {
    const text = `apiKey: ${credentials.apiKey}\napiSecret: ${credentials.apiSecret}`
    try {
      await navigator.clipboard?.writeText(text)
      setCopied(true)
    } catch {
      setCopied(false)
    }
  }

  /** Keeps `aria-modal` honest: Escape closes and Tab stays inside the dialog. */
  function handleKeyDown(event: KeyboardEvent<HTMLDivElement>) {
    if (event.key === 'Escape') {
      event.stopPropagation()
      onClose()
      return
    }
    if (event.key !== 'Tab') return

    const focusable = Array.from(
      dialogRef.current?.querySelectorAll<HTMLElement>(FOCUSABLE) ?? [],
    )
    if (focusable.length === 0) return

    const first = focusable[0]
    const last = focusable[focusable.length - 1]
    const active = document.activeElement

    if (event.shiftKey && (active === first || active === dialogRef.current)) {
      event.preventDefault()
      last.focus()
    } else if (!event.shiftKey && active === last) {
      event.preventDefault()
      first.focus()
    }
  }

  return (
    <div
      ref={dialogRef}
      role="dialog"
      aria-modal="true"
      aria-label={`Credentials for ${credentials.name}`}
      tabIndex={-1}
      onKeyDown={handleKeyDown}
    >
      <h2>Credentials for {credentials.name}</h2>
      <p role="alert">
        This secret is shown only once and cannot be retrieved again. Store it now.
      </p>

      <dl>
        <dt>API key</dt>
        <dd>{credentials.apiKey}</dd>
        <dt>API secret</dt>
        <dd>{credentials.apiSecret}</dd>
      </dl>

      <button type="button" onClick={handleCopy}>
        Copy credentials
      </button>
      {copied && <p role="status">Copied to clipboard.</p>}

      <button type="button" onClick={onClose}>
        Close
      </button>
    </div>
  )
}
