import { describe, expect, it } from 'vitest'
import { createTask, getTask, listTasks, updateTask } from './client'
import { ApiConflictError, ApiNotFoundError, ApiValidationError } from './errors'
import { seedTask } from '../mocks/handlers'

describe('task API client', () => {
  it('lists tasks newest first, as returned by the API', async () => {
    seedTask({ id: 'a', name: 'Oldest' })
    seedTask({ id: 'b', name: 'Newest' })

    const tasks = await listTasks()

    expect(tasks.map((t) => t.id)).toEqual(['b', 'a'])
  })

  it('fetches a single task by id', async () => {
    seedTask({ id: 'abc', name: 'Fetched task' })

    const task = await getTask('abc')

    expect(task.name).toBe('Fetched task')
  })

  it('throws ApiNotFoundError when fetching an unknown id', async () => {
    await expect(getTask('missing')).rejects.toBeInstanceOf(ApiNotFoundError)
  })

  it('creates a task from the minimal required fields', async () => {
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

  it('throws ApiConflictError on duplicate name (case-insensitive)', async () => {
    seedTask({ name: 'Backup' })

    await expect(
      createTask({ name: 'backup', service: 'svc', script: '/s.sh' }),
    ).rejects.toBeInstanceOf(ApiConflictError)
  })

  it('updates a task and returns the updated fields', async () => {
    const existing = seedTask({ name: 'Old name', status: 'CREATED' })

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
    await expect(
      updateTask('missing', { name: 'x', service: 'y', script: 'z' }),
    ).rejects.toBeInstanceOf(ApiNotFoundError)
  })
})
