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
import { parseBearerToken } from './mockAuth'
import { listRegisteredServices } from './serviceHandlers'

// In-memory task store backing the MSW handlers, mirroring the contract in
// api/openapi.yaml (validation rules, 404 and 409 behavior, ordering, and the
// auth rules for writes: anonymous 401, USER-role 403, ADMIN requires a
// registered `service`). The UI never authenticates with service credentials
// (that identity is for machines only), so only the Bearer/ADMIN branch is
// mocked here.

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

/** A Spring cron expression has exactly six whitespace-separated fields. */
const SIX_FIELD_CRON = /^\S+(?:\s+\S+){5}$/

function isValidCron(expression: string | null | undefined): boolean {
  return expression != null && SIX_FIELD_CRON.test(expression.trim())
}

function validate(payload: TaskRequest): FieldError[] {
  const errors: FieldError[] = []

  if (!payload.name || !payload.name.trim()) {
    errors.push({ field: 'name', message: 'must not be blank' })
  }
  if (!payload.service || !payload.service.trim()) {
    errors.push({ field: 'service', message: 'must not be blank' })
  } else if (!listRegisteredServices().some((s) => s.name === payload.service)) {
    errors.push({ field: 'service', message: 'must name a registered service' })
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
  if (payload.scheduled && !isValidCron(payload.cronExpr)) {
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

/**
 * `name` is unique case-insensitively *per owning service*: two different
 * services may each own a task with the same name (see the `DuplicateName`
 * response in api/openapi.yaml).
 */
function findDuplicate(
  name: string,
  service: string,
  excludeId?: string,
): Task | undefined {
  return tasks.find(
    (task) =>
      task.id !== excludeId &&
      task.service === service &&
      task.name.toLowerCase() === name.toLowerCase(),
  )
}

function duplicateName(name: string, service: string) {
  const body: ErrorResponse = {
    message: `A task with name '${name}' already exists for service '${service}'`,
  }
  return HttpResponse.json(body, { status: 409 })
}

function nextId(): string {
  sequence += 1
  return `generated-${sequence}`
}

function unauthenticated() {
  const body: ErrorResponse = { message: 'Authentication required' }
  return HttpResponse.json(body, { status: 401 })
}

function forbidden() {
  const body: ErrorResponse = { message: 'Access denied' }
  return HttpResponse.json(body, { status: 403 })
}

/** Task writes require an ADMIN Bearer token; the UI never sends Basic auth. */
function requireAdminWriter(authorizationHeader: string | null) {
  const caller = parseBearerToken(authorizationHeader)
  if (!caller) return { error: unauthenticated() }
  if (caller.role !== 'ADMIN') return { error: forbidden() }
  return { caller }
}

export const taskHandlers = [
  http.get('/api/tasks', ({ request }) => {
    const url = new URL(request.url)
    const serviceFilter = url.searchParams.get('service')
    const filtered = serviceFilter
      ? tasks.filter((task) => task.service === serviceFilter)
      : tasks
    const ordered = [...filtered].sort((a, b) =>
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
    const auth = requireAdminWriter(request.headers.get('Authorization'))
    if (auth.error) return auth.error

    const payload = (await request.json()) as TaskRequest
    const errors = validate(payload)
    if (errors.length > 0) {
      const body: ValidationErrorResponse = {
        message: 'Validation failed',
        errors,
      }
      return HttpResponse.json(body, { status: 400 })
    }
    if (findDuplicate(payload.name, payload.service)) {
      return duplicateName(payload.name, payload.service)
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
    const auth = requireAdminWriter(request.headers.get('Authorization'))
    if (auth.error) return auth.error

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
    if (findDuplicate(payload.name, payload.service, existing.id)) {
      return duplicateName(payload.name, payload.service)
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
