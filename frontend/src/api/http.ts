import { getSession } from '../auth/session'
import type { ErrorResponse, ValidationErrorResponse } from './types'
import {
  ApiConflictError,
  ApiForbiddenError,
  ApiNotFoundError,
  ApiUnauthorizedError,
  ApiValidationError,
} from './errors'

/** `Authorization: Bearer <token>` header for the current session, or none. */
export function authHeaders(): Record<string, string> {
  const session = getSession()
  return session ? { Authorization: `Bearer ${session.token}` } : {}
}

/** Maps a non-ok API response to the matching typed error and throws it. */
export async function handleErrorResponse(response: Response): Promise<never> {
  if (response.status === 400) {
    const body = (await response.json()) as ValidationErrorResponse
    throw new ApiValidationError(body.message, body.errors)
  }
  if (response.status === 401) {
    const body = (await response.json()) as ErrorResponse
    throw new ApiUnauthorizedError(body.message)
  }
  if (response.status === 403) {
    const body = (await response.json()) as ErrorResponse
    throw new ApiForbiddenError(body.message)
  }
  if (response.status === 404) {
    const body = (await response.json()) as ErrorResponse
    throw new ApiNotFoundError(body.message)
  }
  if (response.status === 409) {
    const body = (await response.json()) as ErrorResponse
    throw new ApiConflictError(body.message)
  }
  throw new Error(`Unexpected API error: ${response.status}`)
}
