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

export function getSession(): Session | null {
  const raw = sessionStorage.getItem(STORAGE_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw) as Session
  } catch {
    return null
  }
}

export function setSession(session: Session): void {
  sessionStorage.setItem(STORAGE_KEY, JSON.stringify(session))
}

export function clearSession(): void {
  sessionStorage.removeItem(STORAGE_KEY)
}
