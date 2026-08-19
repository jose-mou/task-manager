import type {
  ErrorResponse,
  Task,
  TaskList,
  TaskRequest,
  ValidationErrorResponse,
} from './types'
import { ApiConflictError, ApiNotFoundError, ApiValidationError } from './errors'

const BASE_URL = '/api/tasks'

async function handleErrorResponse(response: Response): Promise<never> {
  if (response.status === 400) {
    const body = (await response.json()) as ValidationErrorResponse
    throw new ApiValidationError(body.message, body.errors)
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

export async function listTasks(): Promise<TaskList> {
  const response = await fetch(BASE_URL)
  if (!response.ok) return handleErrorResponse(response)
  return (await response.json()) as TaskList
}

export async function getTask(id: string): Promise<Task> {
  const response = await fetch(`${BASE_URL}/${id}`)
  if (!response.ok) return handleErrorResponse(response)
  return (await response.json()) as Task
}

export async function createTask(payload: TaskRequest): Promise<Task> {
  const response = await fetch(BASE_URL, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  })
  if (!response.ok) return handleErrorResponse(response)
  return (await response.json()) as Task
}

export async function updateTask(id: string, payload: TaskRequest): Promise<Task> {
  const response = await fetch(`${BASE_URL}/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  })
  if (!response.ok) return handleErrorResponse(response)
  return (await response.json()) as Task
}
