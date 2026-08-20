import { expect, test } from '@playwright/test'

// Happy path over the real stack: log in as ADMIN, create a task naming a
// registered service, see it listed, edit it and see the change reflected.
// Task writes are ADMIN-only per the service-registry-and-task-scoping spec,
// so this now logs in first and picks the service from a real registration
// instead of typing free text into what is now a select. Every other case is
// covered by the backend acceptance tests and the frontend unit tests.
test('creates, lists and edits a task', async ({ page, request }) => {
  const name = `E2E smoke ${Date.now()}`
  const updatedName = `${name} edited`
  const serviceName = `e2e-smoke-service-${Date.now()}`

  const login = await request.post('/api/auth/login', {
    data: { username: 'admin', password: 'admin' },
  })
  const { token } = await login.json()
  await request.post('/api/services', {
    headers: { Authorization: `Bearer ${token}` },
    data: { name: serviceName },
  })

  await page.goto('/login')
  await page.getByLabel('Username').fill('admin')
  await page.getByLabel('Password').fill('admin')
  await page.getByRole('button', { name: 'Log in' }).click()
  await expect(page).toHaveURL('/tasks')

  await page.getByRole('link', { name: 'New task' }).click()

  await page.getByLabel('Name', { exact: true }).fill(name)
  await page.getByLabel('Service', { exact: true }).selectOption(serviceName)
  await page.getByLabel('Script', { exact: true }).fill('/opt/scripts/smoke.sh')
  await page.getByRole('button', { name: 'Save' }).click()

  const row = page.getByRole('row').filter({ hasText: name })
  await expect(row).toBeVisible()
  await expect(row).toContainText('CREATED')

  await row.getByRole('link', { name: 'Edit' }).click()
  await page.getByLabel('Name', { exact: true }).fill(updatedName)
  await page.getByLabel('Status', { exact: true }).selectOption('COMPLETED')
  await page.getByRole('button', { name: 'Save' }).click()

  const updatedRow = page.getByRole('row').filter({ hasText: updatedName })
  await expect(updatedRow).toBeVisible()
  await expect(updatedRow).toContainText('COMPLETED')
})
