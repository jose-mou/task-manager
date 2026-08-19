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
