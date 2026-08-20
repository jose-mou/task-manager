import type { PasswordChangeRequest } from './types'
import { authHeaders, handleErrorResponse } from './http'

export async function changeOwnPassword(payload: PasswordChangeRequest): Promise<void> {
  const response = await fetch('/api/users/me/password', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...authHeaders() },
    body: JSON.stringify(payload),
  })
  if (!response.ok) return handleErrorResponse(response)
}
