import type { UserRole } from '../api/types'

// Shared in-memory identity store backing the auth-aware MSW handlers. Tokens
// are not real JWTs (the real backend issues those); they only need to let
// the mock handlers recover a caller's username/role, exactly as the real
// backend would recover them from a genuine JWT.

export interface MockUser {
  username: string
  password: string
  role: UserRole
}

const DEFAULT_ADMIN: MockUser = { username: 'admin', password: 'admin', role: 'ADMIN' }

let users: MockUser[] = [{ ...DEFAULT_ADMIN }]

export function resetUsers(seed: MockUser[] = [DEFAULT_ADMIN]): void {
  users = seed.map((user) => ({ ...user }))
}

export function findUser(username: string): MockUser | undefined {
  return users.find((user) => user.username === username)
}

export function setUserPassword(username: string, newPassword: string): void {
  const user = findUser(username)
  if (user) user.password = newPassword
}

export function issueToken(user: Pick<MockUser, 'username' | 'role'>): string {
  return `mock.${btoa(`${user.username}:${user.role}`)}.token`
}

export interface AuthenticatedUser {
  username: string
  role: UserRole
}

/** Parses `Authorization: Bearer <token>` issued by the mock login handler. */
export function parseBearerToken(
  authorizationHeader: string | null,
): AuthenticatedUser | null {
  if (!authorizationHeader?.startsWith('Bearer ')) return null
  const token = authorizationHeader.slice('Bearer '.length)
  const match = /^mock\.([^.]+)\.token$/.exec(token)
  if (!match) return null
  try {
    const [username, role] = atob(match[1]).split(':')
    if (!username || (role !== 'ADMIN' && role !== 'USER')) return null
    return { username, role }
  } catch {
    return null
  }
}
