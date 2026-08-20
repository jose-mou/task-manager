import { describe, expect, it } from 'vitest'
import {
  deleteService,
  listServices,
  registerService,
  renameService,
  rotateServiceCredentials,
} from './servicesClient'
import { ApiForbiddenError, ApiUnauthorizedError } from './errors'
import { seedService } from '../mocks/handlers'
import { loginAs } from '../test/authHelpers'

describe('services API client', () => {
  it('throws ApiUnauthorizedError when listing services without a session', async () => {
    await expect(listServices()).rejects.toBeInstanceOf(ApiUnauthorizedError)
  })

  it('throws ApiForbiddenError when listing services as a USER', async () => {
    loginAs('USER')
    await expect(listServices()).rejects.toBeInstanceOf(ApiForbiddenError)
  })

  it('lists services ordered by name for an ADMIN', async () => {
    seedService({ name: 'reporting-service' })
    seedService({ name: 'backup-service' })
    loginAs('ADMIN')

    const services = await listServices()

    expect(services.map((s) => s.name)).toEqual(['backup-service', 'reporting-service'])
  })

  it('registers a service and returns one-time credentials', async () => {
    loginAs('ADMIN')

    const result = await registerService({ name: 'backup-service' })

    expect(result.name).toBe('backup-service')
    expect(result.apiKey).toBeTruthy()
    expect(result.apiSecret).toBeTruthy()
  })

  it('renames a service', async () => {
    const service = seedService({ name: 'old-name' })
    loginAs('ADMIN')

    const renamed = await renameService(service.id, { name: 'new-name' })

    expect(renamed.name).toBe('new-name')
  })

  it('deletes a service', async () => {
    const service = seedService({ name: 'to-delete' })
    loginAs('ADMIN')

    await expect(deleteService(service.id)).resolves.toBeUndefined()
    await expect(listServices()).resolves.toEqual([])
  })

  it('rotates credentials, returning a fresh pair', async () => {
    const service = seedService({ name: 'backup-service' })
    loginAs('ADMIN')

    const result = await rotateServiceCredentials(service.id)

    expect(result.id).toBe(service.id)
    expect(result.apiKey).toBeTruthy()
    expect(result.apiSecret).toBeTruthy()
  })
})
