import { http, HttpResponse } from 'msw'
import type {
  ErrorResponse,
  Service,
  ServiceCredentials,
  ServiceRequest,
  ValidationErrorResponse,
} from '../api/types'
import { parseBearerToken } from './mockAuth'

// In-memory service registry backing the MSW handlers, mirroring the contract
// in api/openapi.yaml. Credentials are generated fresh on register/rotate and
// never stored in a form retrievable through a later response.

let services: Service[] = []
let sequence = 0

export function resetServices(seed: Service[] = []): void {
  services = [...seed]
  sequence = 0
}

export function seedService(overrides: Partial<Service> = {}): Service {
  sequence += 1
  const timestamp = new Date(2026, 0, 1, 0, 0, sequence).toISOString()
  const service: Service = {
    id: overrides.id ?? `service-seed-${sequence}`,
    name: overrides.name ?? `service-${sequence}`,
    creationDate: overrides.creationDate ?? timestamp,
    modificationDate: overrides.modificationDate ?? timestamp,
  }
  services.push(service)
  return service
}

function nextId(): string {
  sequence += 1
  return `service-generated-${sequence}`
}

function randomCredentialPart(): string {
  const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_-'
  let value = ''
  for (let i = 0; i < 43; i += 1) {
    value += chars[Math.floor(Math.random() * chars.length)]
  }
  return value
}

function unauthenticated() {
  const body: ErrorResponse = { message: 'Authentication required' }
  return HttpResponse.json(body, { status: 401 })
}

function forbidden() {
  const body: ErrorResponse = { message: 'Access denied' }
  return HttpResponse.json(body, { status: 403 })
}

function notFound(id: string) {
  const body: ErrorResponse = { message: `Service ${id} not found` }
  return HttpResponse.json(body, { status: 404 })
}

function requireAdmin(authorizationHeader: string | null) {
  const caller = parseBearerToken(authorizationHeader)
  if (!caller) return { error: unauthenticated() }
  if (caller.role !== 'ADMIN') return { error: forbidden() }
  return { caller }
}

function validateName(payload: ServiceRequest) {
  if (!payload.name || !payload.name.trim()) {
    const body: ValidationErrorResponse = {
      message: 'Validation failed',
      errors: [{ field: 'name', message: 'must not be blank' }],
    }
    return HttpResponse.json(body, { status: 400 })
  }
  return null
}

export const serviceHandlers = [
  http.get('/api/services', ({ request }) => {
    const auth = requireAdmin(request.headers.get('Authorization'))
    if (auth.error) return auth.error

    const ordered = [...services].sort((a, b) => a.name.localeCompare(b.name))
    return HttpResponse.json(ordered)
  }),

  http.post('/api/services', async ({ request }) => {
    const auth = requireAdmin(request.headers.get('Authorization'))
    if (auth.error) return auth.error

    const payload = (await request.json()) as ServiceRequest
    const invalid = validateName(payload)
    if (invalid) return invalid

    const nowIso = new Date().toISOString()
    const service: Service = {
      id: nextId(),
      name: payload.name,
      creationDate: nowIso,
      modificationDate: nowIso,
    }
    services.push(service)

    const body: ServiceCredentials = {
      id: service.id,
      name: service.name,
      apiKey: randomCredentialPart(),
      apiSecret: randomCredentialPart(),
    }
    return HttpResponse.json(body, { status: 201 })
  }),

  http.put('/api/services/:id', async ({ params, request }) => {
    const auth = requireAdmin(request.headers.get('Authorization'))
    if (auth.error) return auth.error

    const service = services.find((s) => s.id === params.id)
    if (!service) return notFound(String(params.id))

    const payload = (await request.json()) as ServiceRequest
    const invalid = validateName(payload)
    if (invalid) return invalid

    service.name = payload.name
    service.modificationDate = new Date().toISOString()
    return HttpResponse.json(service)
  }),

  http.delete('/api/services/:id', ({ params, request }) => {
    const auth = requireAdmin(request.headers.get('Authorization'))
    if (auth.error) return auth.error

    const service = services.find((s) => s.id === params.id)
    if (!service) return notFound(String(params.id))

    services = services.filter((s) => s.id !== params.id)
    return new HttpResponse(null, { status: 204 })
  }),

  http.post('/api/services/:id/credentials', ({ params, request }) => {
    const auth = requireAdmin(request.headers.get('Authorization'))
    if (auth.error) return auth.error

    const service = services.find((s) => s.id === params.id)
    if (!service) return notFound(String(params.id))

    service.modificationDate = new Date().toISOString()
    const body: ServiceCredentials = {
      id: service.id,
      name: service.name,
      apiKey: randomCredentialPart(),
      apiSecret: randomCredentialPart(),
    }
    return HttpResponse.json(body)
  }),
]

export function listRegisteredServices(): Service[] {
  return services
}
