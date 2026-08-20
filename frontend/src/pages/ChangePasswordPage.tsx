import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { changeOwnPassword } from '../api/usersClient'
import { ApiForbiddenError, ApiUnauthorizedError, ApiValidationError } from '../api/errors'
import type { FieldError } from '../api/types'
import { ChangePasswordForm } from '../components/organisms/ChangePasswordForm'
import { useAuth } from '../auth/AuthContext'

export function ChangePasswordPage() {
  const { logout } = useAuth()
  const navigate = useNavigate()
  const [fieldErrors, setFieldErrors] = useState<FieldError[]>()
  const [submitError, setSubmitError] = useState<string | null>(null)
  const [success, setSuccess] = useState(false)
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(currentPassword: string, newPassword: string) {
    setSubmitting(true)
    setFieldErrors(undefined)
    setSubmitError(null)
    setSuccess(false)
    try {
      await changeOwnPassword({ currentPassword, newPassword })
      setSuccess(true)
    } catch (error) {
      if (error instanceof ApiValidationError) {
        setFieldErrors(error.fieldErrors)
      } else if (error instanceof ApiUnauthorizedError || error instanceof ApiForbiddenError) {
        logout()
        navigate('/login')
      } else {
        setSubmitError('Could not change the password.')
      }
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <section>
      <h1>Change password</h1>
      {success && <p role="status">Password changed successfully.</p>}
      {submitError && <p role="alert">{submitError}</p>}
      <ChangePasswordForm
        fieldErrors={fieldErrors}
        submitting={submitting}
        onSubmit={handleSubmit}
      />
    </section>
  )
}
