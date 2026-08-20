import { http, HttpResponse } from 'msw'
import type {
  ErrorResponse,
  LoginRequest,
  LoginResponse,
  PasswordChangeRequest,
  ValidationErrorResponse,
} from '../api/types'
import { findUser, issueToken, parseBearerToken, setUserPassword } from './mockAuth'

export const authHandlers = [
  http.post('/api/auth/login', async ({ request }) => {
    const payload = (await request.json()) as LoginRequest
    const user = findUser(payload.username)
    if (!user || user.password !== payload.password) {
      const body: ErrorResponse = { message: 'Invalid username or password' }
      return HttpResponse.json(body, { status: 401 })
    }

    const body: LoginResponse = {
      token: issueToken(user),
      expiresAt: new Date(Date.now() + 8 * 60 * 60 * 1000).toISOString(),
      role: user.role,
    }
    return HttpResponse.json(body)
  }),

  http.post('/api/users/me/password', async ({ request }) => {
    const caller = parseBearerToken(request.headers.get('Authorization'))
    if (!caller) {
      const body: ErrorResponse = { message: 'Authentication required' }
      return HttpResponse.json(body, { status: 401 })
    }

    const payload = (await request.json()) as PasswordChangeRequest
    const user = findUser(caller.username)
    if (!user || user.password !== payload.currentPassword) {
      const body: ValidationErrorResponse = {
        message: 'Validation failed',
        errors: [
          { field: 'currentPassword', message: 'does not match the current password' },
        ],
      }
      return HttpResponse.json(body, { status: 400 })
    }
    if (payload.newPassword.length < 8) {
      const body: ValidationErrorResponse = {
        message: 'Validation failed',
        errors: [{ field: 'newPassword', message: 'must be at least 8 characters long' }],
      }
      return HttpResponse.json(body, { status: 400 })
    }
    if (payload.newPassword === payload.currentPassword) {
      const body: ValidationErrorResponse = {
        message: 'Validation failed',
        errors: [
          { field: 'newPassword', message: 'must be different from the current password' },
        ],
      }
      return HttpResponse.json(body, { status: 400 })
    }

    setUserPassword(user.username, payload.newPassword)
    return new HttpResponse(null, { status: 204 })
  }),
]
