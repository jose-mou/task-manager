import { describe, expect, it } from 'vitest'
import { createTask, getTask, listTasks, updateTask } from './client'
import {
  ApiConflictError,
  ApiForbiddenError,
  ApiNotFoundError,
  ApiUnauthorizedError,
  ApiValidationError,
} from './errors'
import { seedService, seedTask } from '../mocks/handlers'
import { loginAs } from '../test/authHelpers'

describe('task API client', () => {
  it('lists tasks newest first, as returned by the API', async () => {
    seedTask({ id: 'a', name: 'Oldest' })
    seedTask({ id: 'b', name: 'Newest' })

    const tasks = await listTasks()

    expect(tasks.map((t) => t.id)).toEqual(['b', 'a'])
  })

  it('filters the list by the given service name', async () => {
    seedTask({ id: 'a', name: 'Backup job', service: 'backup-service' })
    seedTask({ id: 'b', name: 'Report job', service: 'reporting-service' })

    const tasks = await listTasks('backup-service')

    expect(tasks.map((t) => t.id)).toEqual(['a'])
  })

  it('fetches a single task by id', async () => {
    seedTask({ id: 'abc', name: 'Fetched task' })

    const task = await getTask('abc')

    expect(task.name).toBe('Fetched task')
  })

  it('throws ApiNotFoundError when fetching an unknown id', async () => {
    await expect(getTask('missing')).rejects.toBeInstanceOf(ApiNotFoundError)
  })

  it('throws ApiUnauthorizedError when creating a task without a session', async () => {
    seedService({ name: 'reporting-service' })

    await expect(
      createTask({
        name: 'Report generation',
        service: 'reporting-service',
        script: '/opt/scripts/report.sh',
      }),
    ).rejects.toBeInstanceOf(ApiUnauthorizedError)
  })

  it('throws ApiForbiddenError when a USER-role session creates a task', async () => {
    seedService({ name: 'reporting-service' })
    loginAs('USER')

    await expect(
      createTask({
        name: 'Report generation',
        service: 'reporting-service',
        script: '/opt/scripts/report.sh',
      }),
    ).rejects.toBeInstanceOf(ApiForbiddenError)
  })

  it('creates a task from the minimal required fields as an ADMIN', async () => {
    seedService({ name: 'reporting-service' })
    loginAs('ADMIN')

    const task = await createTask({
      name: 'Report generation',
      service: 'reporting-service',
      script: '/opt/scripts/report.sh',
    })

    expect(task.id).toBeTruthy()
    expect(task.status).toBe('CREATED')
    expect(task.scheduled).toBe(false)
    expect(task.creationDate).toBe(task.modificationDate)
  })

  it('throws ApiValidationError with every offending field on 400', async () => {
    loginAs('ADMIN')
    try {
      await createTask({ name: '', service: '', script: 'x', maxExecutions: 0 })
      expect.unreachable('createTask should have thrown')
    } catch (error) {
      expect(error).toBeInstanceOf(ApiValidationError)
      const validationError = error as ApiValidationError
      const fields = validationError.fieldErrors.map((e) => e.field)
      expect(fields).toEqual(
        expect.arrayContaining(['name', 'service', 'maxExecutions']),
      )
    }
  })

  it('throws a 400 naming service when it does not name a registered service', async () => {
    loginAs('ADMIN')
    try {
      await createTask({ name: 'x', service: 'unknown-service', script: 'y' })
      expect.unreachable('createTask should have thrown')
    } catch (error) {
      expect(error).toBeInstanceOf(ApiValidationError)
      const fields = (error as ApiValidationError).fieldErrors.map((e) => e.field)
      expect(fields).toContain('service')
    }
  })

  it('throws ApiConflictError on a duplicate name within the same service (case-insensitive)', async () => {
    seedService({ name: 'svc' })
    seedTask({ name: 'Backup', service: 'svc' })
    loginAs('ADMIN')

    await expect(
      createTask({ name: 'backup', service: 'svc', script: '/s.sh' }),
    ).rejects.toBeInstanceOf(ApiConflictError)
  })

  it('accepts the same task name for a different owning service', async () => {
    seedService({ name: 'backup-service' })
    seedService({ name: 'reporting-service' })
    seedTask({ name: 'Backup', service: 'backup-service' })
    loginAs('ADMIN')

    const task = await createTask({
      name: 'Backup',
      service: 'reporting-service',
      script: '/s.sh',
    })

    expect(task.service).toBe('reporting-service')
  })

  it('updates a task and returns the updated fields', async () => {
    seedService({ name: 'some-service' })
    const existing = seedTask({ name: 'Old name', status: 'CREATED' })
    loginAs('ADMIN')

    const updated = await updateTask(existing.id, {
      name: 'New name',
      service: existing.service,
      script: existing.script,
      status: 'COMPLETED',
    })

    expect(updated.name).toBe('New name')
    expect(updated.status).toBe('COMPLETED')
    expect(updated.creationDate).toBe(existing.creationDate)
  })

  it('throws ApiNotFoundError when updating an unknown id', async () => {
    seedService({ name: 'y' })
    loginAs('ADMIN')
    await expect(
      updateTask('missing', { name: 'x', service: 'y', script: 'z' }),
    ).rejects.toBeInstanceOf(ApiNotFoundError)
  })
})
