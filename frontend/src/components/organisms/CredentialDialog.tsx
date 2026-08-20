import { useState } from 'react'
import type { ServiceCredentials } from '../../api/types'

interface CredentialDialogProps {
  credentials: ServiceCredentials
  onClose: () => void
}

/**
 * One-time credential dialog: the caller renders this only while it holds a
 * freshly received `ServiceCredentials` response in local state. There is no
 * way to reopen it from stored data — once the response is gone, so is the
 * secret, exactly as the API never returns it again.
 */
export function CredentialDialog({ credentials, onClose }: CredentialDialogProps) {
  const [copied, setCopied] = useState(false)

  async function handleCopy() {
    const text = `apiKey: ${credentials.apiKey}\napiSecret: ${credentials.apiSecret}`
    try {
      await navigator.clipboard?.writeText(text)
      setCopied(true)
    } catch {
      setCopied(false)
    }
  }

  return (
    <div role="dialog" aria-modal="true" aria-label={`Credentials for ${credentials.name}`}>
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
