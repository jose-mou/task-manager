import type { UserRole } from '../api/types'
import { setSession } from '../auth/session'
import { issueToken } from '../mocks/mockAuth'

/** Test helper: stores a session as if the given role had just logged in. */
export function loginAs(role: UserRole, username = role.toLowerCase()): void {
  setSession({
    token: issueToken({ username, role }),
    expiresAt: new Date(Date.now() + 8 * 60 * 60 * 1000).toISOString(),
    role,
  })
}
