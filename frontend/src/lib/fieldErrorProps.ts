export function fieldErrorProps(htmlFor: string, error?: string) {
  return error
    ? { 'aria-invalid': true as const, 'aria-describedby': `${htmlFor}-error` }
    : {}
}
