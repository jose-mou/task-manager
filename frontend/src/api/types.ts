// Types mirroring api/openapi.yaml. Keep in sync with the frozen contract.

export type TaskStatus = 'CREATED' | 'RUNNING' | 'COMPLETED' | 'CANCELED'

export const TASK_STATUSES: TaskStatus[] = [
  'CREATED',
  'RUNNING',
  'COMPLETED',
  'CANCELED',
]

/** Payload accepted by POST /api/tasks and PUT /api/tasks/{id}. */
export interface TaskRequest {
  name: string
  service: string
  description?: string | null
  status?: TaskStatus
  script: string
  cronExpr?: string | null
  maxExecutions?: number | null
  scheduled?: boolean
}

/** A stored task, as returned by the API. */
export interface Task {
  id: string
  name: string
  creationDate: string
  modificationDate: string
  service: string
  description: string | null
  status: TaskStatus
  script: string
  cronExpr: string | null
  maxExecutions: number | null
  scheduled: boolean
}

export type TaskList = Task[]

export interface FieldError {
  field: string
  message: string
}

export interface ValidationErrorResponse {
  message: string
  errors: FieldError[]
}

export interface ErrorResponse {
  message: string
}

// --- Authentication & users -------------------------------------------------

export type UserRole = 'ADMIN' | 'USER'

/** Payload accepted by POST /api/auth/login. */
export interface LoginRequest {
  username: string
  password: string
}

/** Response of POST /api/auth/login. */
export interface LoginResponse {
  token: string
  expiresAt: string
  role: UserRole
}

/** Payload accepted by POST /api/users/me/password. */
export interface PasswordChangeRequest {
  currentPassword: string
  newPassword: string
}

// --- Service registry ---------------------------------------------------

/** Payload accepted by POST /api/services and PUT /api/services/{id}. */
export interface ServiceRequest {
  name: string
}

/** A registered service, without any credential value. */
export interface Service {
  id: string
  name: string
  creationDate: string
  modificationDate: string
}

export type ServiceList = Service[]

/** One-time credential payload returned by register and rotate. */
export interface ServiceCredentials {
  id: string
  name: string
  apiKey: string
  apiSecret: string
}
