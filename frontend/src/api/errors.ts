import type { FieldError } from './types'

/** Thrown when the API returns 400 with a ValidationErrorResponse payload. */
export class ApiValidationError extends Error {
  readonly fieldErrors: FieldError[]

  constructor(message: string, fieldErrors: FieldError[]) {
    super(message)
    this.name = 'ApiValidationError'
    this.fieldErrors = fieldErrors
  }
}

/** Thrown when the API returns 409 (duplicate name). */
export class ApiConflictError extends Error {
  constructor(message: string) {
    super(message)
    this.name = 'ApiConflictError'
  }
}

/** Thrown when the API returns 404 (unknown task id). */
export class ApiNotFoundError extends Error {
  constructor(message: string) {
    super(message)
    this.name = 'ApiNotFoundError'
  }
}

/** Thrown when the API returns 401 (missing, malformed or expired credentials). */
export class ApiUnauthorizedError extends Error {
  constructor(message: string) {
    super(message)
    this.name = 'ApiUnauthorizedError'
  }
}

/** Thrown when the API returns 403 (authenticated but not allowed). */
export class ApiForbiddenError extends Error {
  constructor(message: string) {
    super(message)
    this.name = 'ApiForbiddenError'
  }
}
