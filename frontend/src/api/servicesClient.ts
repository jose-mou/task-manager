import type { Service, ServiceCredentials, ServiceList, ServiceRequest } from './types'
import { authHeaders, handleErrorResponse } from './http'

const BASE_URL = '/api/services'

export async function listServices(): Promise<ServiceList> {
  const response = await fetch(BASE_URL, { headers: { ...authHeaders() } })
  if (!response.ok) return handleErrorResponse(response)
  return (await response.json()) as ServiceList
}

export async function registerService(payload: ServiceRequest): Promise<ServiceCredentials> {
  const response = await fetch(BASE_URL, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...authHeaders() },
    body: JSON.stringify(payload),
  })
  if (!response.ok) return handleErrorResponse(response)
  return (await response.json()) as ServiceCredentials
}

export async function renameService(id: string, payload: ServiceRequest): Promise<Service> {
  const response = await fetch(`${BASE_URL}/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json', ...authHeaders() },
    body: JSON.stringify(payload),
  })
  if (!response.ok) return handleErrorResponse(response)
  return (await response.json()) as Service
}

export async function deleteService(id: string): Promise<void> {
  const response = await fetch(`${BASE_URL}/${id}`, {
    method: 'DELETE',
    headers: { ...authHeaders() },
  })
  if (!response.ok) return handleErrorResponse(response)
}

export async function rotateServiceCredentials(id: string): Promise<ServiceCredentials> {
  const response = await fetch(`${BASE_URL}/${id}/credentials`, {
    method: 'POST',
    headers: { ...authHeaders() },
  })
  if (!response.ok) return handleErrorResponse(response)
  return (await response.json()) as ServiceCredentials
}
