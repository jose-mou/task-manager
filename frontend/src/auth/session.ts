import type { UserRole } from '../api/types'

// The JWT lives only in sessionStorage: cleared when the tab closes, never
// sent anywhere but the Authorization header, never decoded by the UI (the
// login response's `role` field is the source of truth for admin nav).

const STORAGE_KEY = 'task-manager.session'

export interface Session {
  token: string
  expiresAt: string
  role: UserRole
}

function isExpired(expiresAt: unknown): boolean {
  if (typeof expiresAt !== 'string') return true
  const instant = Date.parse(expiresAt)
  return Number.isNaN(instant) || instant <= Date.now()
}

/**
 * The stored session, or `null` when there is none or the token has already
 * expired. There is no refresh token: an expired session is dropped from
 * storage so the UI falls straight back to the anonymous, read-only views
 * instead of showing admin controls that every write would reject with 401.
 */
export function getSession(): Session | null {
  const raw = sessionStorage.getItem(STORAGE_KEY)
  if (!raw) return null
  let session: Session
  try {
    session = JSON.parse(raw) as Session
  } catch {
    clearSession()
    return null
  }
  if (!session?.token || isExpired(session.expiresAt)) {
    clearSession()
    return null
  }
  return session
}

export function setSession(session: Session): void {
  sessionStorage.setItem(STORAGE_KEY, JSON.stringify(session))
}

export function clearSession(): void {
  sessionStorage.removeItem(STORAGE_KEY)
}
