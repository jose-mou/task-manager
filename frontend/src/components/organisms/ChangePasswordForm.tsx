import { useState, type FormEvent } from 'react'
import type { FieldError } from '../../api/types'
import { FormField } from '../molecules/FormField'
import { Input } from '../atoms/Input'

interface ChangePasswordFormProps {
  fieldErrors?: FieldError[]
  submitting?: boolean
  onSubmit: (currentPassword: string, newPassword: string) => void
}

export function ChangePasswordForm({
  fieldErrors,
  submitting = false,
  onSubmit,
}: ChangePasswordFormProps) {
  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')

  const errorFor = (field: string) =>
    fieldErrors?.find((error) => error.field === field)?.message

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    onSubmit(currentPassword, newPassword)
  }

  return (
    <form onSubmit={handleSubmit} noValidate>
      <FormField
        htmlFor="currentPassword"
        label="Current password"
        error={errorFor('currentPassword')}
      >
        {(control) => (
          <Input
            {...control}
            type="password"
            required
            value={currentPassword}
            onChange={(e) => setCurrentPassword(e.target.value)}
          />
        )}
      </FormField>

      <FormField
        htmlFor="newPassword"
        label="New password"
        error={errorFor('newPassword')}
      >
        {(control) => (
          <Input
            {...control}
            type="password"
            required
            minLength={8}
            value={newPassword}
            onChange={(e) => setNewPassword(e.target.value)}
          />
        )}
      </FormField>

      <button type="submit" disabled={submitting}>
        Change password
      </button>
    </form>
  )
}
