import { http, HttpResponse } from 'msw'
import type {
  ErrorResponse,
  FieldError,
  Task,
  TaskRequest,
  TaskStatus,
  ValidationErrorResponse,
} from '../api/types'
import { TASK_STATUSES } from '../api/types'

// In-memory task store backing the MSW handlers, mirroring the contract in
// api/openapi.yaml (validation rules, 404 and 409 behavior, ordering).

let tasks: Task[] = []
let sequence = 0

export function resetTasks(seed: Task[] = []): void {
  tasks = [...seed]
  sequence = 0
}

export function seedTask(overrides: Partial<Task> = {}): Task {
  sequence += 1
  const timestamp = new Date(2026, 0, 1, 0, 0, sequence).toISOString()
  const task: Task = {
    id: overrides.id ?? `seed-${sequence}`,
    name: overrides.name ?? `Task ${sequence}`,
    creationDate: overrides.creationDate ?? timestamp,
    modificationDate: overrides.modificationDate ?? timestamp,
    service: overrides.service ?? 'some-service',
    description: overrides.description ?? null,
    status: overrides.status ?? 'CREATED',
    script: overrides.script ?? '/opt/scripts/run.sh',
    cronExpr: overrides.cronExpr ?? null,
    maxExecutions: overrides.maxExecutions ?? null,
    scheduled: overrides.scheduled ?? false,
  }
  tasks.push(task)
  return task
}

function validate(payload: TaskRequest): FieldError[] {
  const errors: FieldError[] = []

  if (!payload.name || !payload.name.trim()) {
    errors.push({ field: 'name', message: 'must not be blank' })
  }
  if (!payload.service || !payload.service.trim()) {
    errors.push({ field: 'service', message: 'must not be blank' })
  }
  if (!payload.script || !payload.script.trim()) {
    errors.push({ field: 'script', message: 'must not be blank' })
  }
  if (
    payload.status !== undefined &&
    !TASK_STATUSES.includes(payload.status as TaskStatus)
  ) {
    errors.push({
      field: 'status',
      message: 'must be one of CREATED, RUNNING, COMPLETED, CANCELED',
    })
  }
  if (payload.scheduled && (!payload.cronExpr || !payload.cronExpr.trim())) {
    errors.push({
      field: 'cronExpr',
      message:
        'must be a valid 6-field cron expression when the task is scheduled',
    })
  }
  if (payload.maxExecutions != null && payload.maxExecutions < 1) {
    errors.push({
      field: 'maxExecutions',
      message: 'must be greater than or equal to 1',
    })
  }

  return errors
}

function findDuplicate(name: string, excludeId?: string): Task | undefined {
  return tasks.find(
    (task) =>
      task.id !== excludeId && task.name.toLowerCase() === name.toLowerCase(),
  )
}

function nextId(): string {
  sequence += 1
  return `generated-${sequence}`
}

export const handlers = [
  http.get('/api/tasks', () => {
    const ordered = [...tasks].sort((a, b) =>
      b.creationDate.localeCompare(a.creationDate),
    )
    return HttpResponse.json(ordered)
  }),

  http.get('/api/tasks/:id', ({ params }) => {
    const task = tasks.find((t) => t.id === params.id)
    if (!task) {
      const body: ErrorResponse = { message: `Task ${params.id} not found` }
      return HttpResponse.json(body, { status: 404 })
    }
    return HttpResponse.json(task)
  }),

  http.post('/api/tasks', async ({ request }) => {
    const payload = (await request.json()) as TaskRequest
    const errors = validate(payload)
    if (errors.length > 0) {
      const body: ValidationErrorResponse = {
        message: 'Validation failed',
        errors,
      }
      return HttpResponse.json(body, { status: 400 })
    }
    if (findDuplicate(payload.name)) {
      const body: ErrorResponse = {
        message: `A task with name '${payload.name}' already exists`,
      }
      return HttpResponse.json(body, { status: 409 })
    }

    const nowIso = new Date().toISOString()
    const task: Task = {
      id: nextId(),
      name: payload.name,
      creationDate: nowIso,
      modificationDate: nowIso,
      service: payload.service,
      description: payload.description ?? null,
      status: payload.status ?? 'CREATED',
      script: payload.script,
      cronExpr: payload.cronExpr ?? null,
      maxExecutions: payload.maxExecutions ?? null,
      scheduled: payload.scheduled ?? false,
    }
    tasks.push(task)
    return HttpResponse.json(task, { status: 201 })
  }),

  http.put('/api/tasks/:id', async ({ params, request }) => {
    const existing = tasks.find((t) => t.id === params.id)
    if (!existing) {
      const body: ErrorResponse = { message: `Task ${params.id} not found` }
      return HttpResponse.json(body, { status: 404 })
    }

    const payload = (await request.json()) as TaskRequest
    const errors = validate(payload)
    if (errors.length > 0) {
      const body: ValidationErrorResponse = {
        message: 'Validation failed',
        errors,
      }
      return HttpResponse.json(body, { status: 400 })
    }
    if (findDuplicate(payload.name, existing.id)) {
      const body: ErrorResponse = {
        message: `A task with name '${payload.name}' already exists`,
      }
      return HttpResponse.json(body, { status: 409 })
    }

    existing.name = payload.name
    existing.service = payload.service
    existing.description = payload.description ?? null
    existing.status = payload.status ?? 'CREATED'
    existing.script = payload.script
    existing.cronExpr = payload.cronExpr ?? null
    existing.maxExecutions = payload.maxExecutions ?? null
    existing.scheduled = payload.scheduled ?? false
    existing.modificationDate = new Date().toISOString()

    return HttpResponse.json(existing)
  }),
]
