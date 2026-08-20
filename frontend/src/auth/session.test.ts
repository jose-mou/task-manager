import { describe, expect, it } from 'vitest'
import { clearSession, getSession, setSession } from './session'

const IN_EIGHT_HOURS = () => new Date(Date.now() + 8 * 60 * 60 * 1000).toISOString()
const AN_HOUR_AGO = () => new Date(Date.now() - 60 * 60 * 1000).toISOString()

describe('session storage', () => {
  it('returns null when nothing is stored', () => {
    expect(getSession()).toBeNull()
  })

  it('round-trips a valid session', () => {
    setSession({ token: 'jwt-value', expiresAt: IN_EIGHT_HOURS(), role: 'ADMIN' })

    expect(getSession()).toMatchObject({ token: 'jwt-value', role: 'ADMIN' })
  })

  it('keeps the role from the login response instead of decoding the token', () => {
    // A token whose payload claims ADMIN must not promote a USER session.
    const adminClaim = btoa(JSON.stringify({ sub: 'someone', role: 'ADMIN' }))
    setSession({
      token: `header.${adminClaim}.signature`,
      expiresAt: IN_EIGHT_HOURS(),
      role: 'USER',
    })

    expect(getSession()?.role).toBe('USER')
  })

  it('drops an expired session instead of reporting it as logged in', () => {
    setSession({ token: 'jwt-value', expiresAt: AN_HOUR_AGO(), role: 'ADMIN' })

    expect(getSession()).toBeNull()
    expect(sessionStorage.getItem('task-manager.session')).toBeNull()
  })

  it('drops a session whose expiry is unusable', () => {
    sessionStorage.setItem(
      'task-manager.session',
      JSON.stringify({ token: 'jwt-value', expiresAt: 'not-a-date', role: 'ADMIN' }),
    )

    expect(getSession()).toBeNull()
  })

  it('drops corrupted stored content', () => {
    sessionStorage.setItem('task-manager.session', 'not-json')

    expect(getSession()).toBeNull()
    expect(sessionStorage.getItem('task-manager.session')).toBeNull()
  })

  it('clears the stored session on logout', () => {
    setSession({ token: 'jwt-value', expiresAt: IN_EIGHT_HOURS(), role: 'ADMIN' })

    clearSession()

    expect(getSession()).toBeNull()
  })
})
