import type { ReactNode } from 'react'
import { Label } from '../atoms/Label'
import { ErrorText } from '../atoms/ErrorText'

interface FormFieldProps {
  htmlFor: string
  label: string
  error?: string
  children: ReactNode
}

export function FormField({ htmlFor, label, error, children }: FormFieldProps) {
  const errorId = `${htmlFor}-error`
  return (
    <div className="form-field">
      <Label htmlFor={htmlFor}>{label}</Label>
      {children}
      <ErrorText id={errorId} message={error} />
    </div>
  )
}
