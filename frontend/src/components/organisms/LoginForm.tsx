import { useState, type FormEvent } from 'react'
import { FormField } from '../molecules/FormField'
import { Input } from '../atoms/Input'

interface LoginFormProps {
  error?: string | null
  submitting?: boolean
  onSubmit: (username: string, password: string) => void
}

export function LoginForm({ error, submitting = false, onSubmit }: LoginFormProps) {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    onSubmit(username, password)
  }

  return (
    <form onSubmit={handleSubmit} noValidate>
      {error && <p role="alert">{error}</p>}

      <FormField htmlFor="username" label="Username">
        {(control) => (
          <Input
            {...control}
            required
            value={username}
            onChange={(e) => setUsername(e.target.value)}
          />
        )}
      </FormField>

      <FormField htmlFor="password" label="Password">
        {(control) => (
          <Input
            {...control}
            type="password"
            required
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
        )}
      </FormField>

      <button type="submit" disabled={submitting}>
        Log in
      </button>
    </form>
  )
}
