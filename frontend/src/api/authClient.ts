import type { LoginRequest, LoginResponse } from './types'
import { handleErrorResponse } from './http'

export async function login(payload: LoginRequest): Promise<LoginResponse> {
  const response = await fetch('/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  })
  if (!response.ok) return handleErrorResponse(response)
  return (await response.json()) as LoginResponse
}
