import { describe, expect, it } from 'vitest'
import { login } from './authClient'
import { ApiUnauthorizedError } from './errors'

describe('auth API client', () => {
  it('returns a token, expiry and role for the seeded admin', async () => {
    const response = await login({ username: 'admin', password: 'admin' })

    expect(response.token).toBeTruthy()
    expect(response.role).toBe('ADMIN')
    expect(response.expiresAt).toBeTruthy()
  })

  it('throws ApiUnauthorizedError for a wrong password', async () => {
    await expect(login({ username: 'admin', password: 'wrong' })).rejects.toBeInstanceOf(
      ApiUnauthorizedError,
    )
  })

  it('throws ApiUnauthorizedError for an unknown username', async () => {
    await expect(login({ username: 'ghost', password: 'x' })).rejects.toBeInstanceOf(
      ApiUnauthorizedError,
    )
  })
})
