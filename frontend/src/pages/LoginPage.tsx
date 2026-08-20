import { useState } from 'react'
import { useLocation, useNavigate, type Location } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { LoginForm } from '../components/organisms/LoginForm'
import { ApiUnauthorizedError } from '../api/errors'

interface LocationState {
  from?: Location
}

export function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(username: string, password: string) {
    setSubmitting(true)
    setError(null)
    try {
      await login(username, password)
      const state = location.state as LocationState | null
      const redirectTo = state?.from
        ? `${state.from.pathname}${state.from.search}`
        : '/tasks'
      navigate(redirectTo, { replace: true })
    } catch (err) {
      if (err instanceof ApiUnauthorizedError) {
        setError('Invalid username or password.')
      } else {
        setError('Could not log in.')
      }
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <section>
      <h1>Log in</h1>
      <LoginForm error={error} submitting={submitting} onSubmit={handleSubmit} />
    </section>
  )
}
