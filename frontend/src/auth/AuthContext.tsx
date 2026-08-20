import { createContext, useContext, useMemo, useState, type ReactNode } from 'react'
import { login as loginRequest } from '../api/authClient'
import { clearSession, getSession, setSession, type Session } from './session'

interface AuthContextValue {
  session: Session | null
  isAdmin: boolean
  login: (username: string, password: string) => Promise<void>
  logout: () => void
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSessionState] = useState<Session | null>(() => getSession())

  async function login(username: string, password: string) {
    const response = await loginRequest({ username, password })
    const next: Session = {
      token: response.token,
      expiresAt: response.expiresAt,
      role: response.role,
    }
    setSession(next)
    setSessionState(next)
  }

  function logout() {
    clearSession()
    setSessionState(null)
  }

  const value = useMemo<AuthContextValue>(
    () => ({ session, isAdmin: session?.role === 'ADMIN', login, logout }),
    [session],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return context
}
