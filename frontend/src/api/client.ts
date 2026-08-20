import type { Task, TaskList, TaskRequest } from './types'
import { authHeaders, handleErrorResponse } from './http'

const BASE_URL = '/api/tasks'

export async function listTasks(service?: string): Promise<TaskList> {
  const url = service ? `${BASE_URL}?service=${encodeURIComponent(service)}` : BASE_URL
  const response = await fetch(url)
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
    headers: { 'Content-Type': 'application/json', ...authHeaders() },
    body: JSON.stringify(payload),
  })
  if (!response.ok) return handleErrorResponse(response)
  return (await response.json()) as Task
}

export async function updateTask(id: string, payload: TaskRequest): Promise<Task> {
  const response = await fetch(`${BASE_URL}/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json', ...authHeaders() },
    body: JSON.stringify(payload),
  })
  if (!response.ok) return handleErrorResponse(response)
  return (await response.json()) as Task
}
