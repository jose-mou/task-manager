import type { ReactNode } from 'react'
import { Label } from '../atoms/Label'
import { ErrorText } from '../atoms/ErrorText'

/**
 * Props the field wires into its control so that the label, the control and the
 * error message stay associated for assistive technology.
 */
export interface FieldControlProps {
  id: string
  'aria-invalid'?: true
  'aria-describedby'?: string
}

interface FormFieldProps {
  htmlFor: string
  label: string
  error?: string
  children: (control: FieldControlProps) => ReactNode
}

export function FormField({ htmlFor, label, error, children }: FormFieldProps) {
  const errorId = `${htmlFor}-error`
  const control: FieldControlProps = error
    ? { id: htmlFor, 'aria-invalid': true, 'aria-describedby': errorId }
    : { id: htmlFor }

  return (
    <div className="form-field">
      <Label htmlFor={htmlFor}>{label}</Label>
      {children(control)}
      <ErrorText id={errorId} message={error} />
    </div>
  )
}
