import { expect, test } from '@playwright/test'

// Happy path over the real stack: log in as ADMIN, register a service and see
// its one-time credentials, then confirm the public task list stays reachable
// with no session at all. Everything else (rotation, scoping, 403s, the
// exact 400/409 payloads) is covered by the backend acceptance tests and the
// frontend unit tests.
test('admin registers a service and the public task list needs no session', async ({
  page,
  browser,
}) => {
  const serviceName = `e2e-registry-service-${Date.now()}`

  await page.goto('/login')
  await page.getByLabel('Username').fill('admin')
  await page.getByLabel('Password').fill('admin')
  await page.getByRole('button', { name: 'Log in' }).click()
  await expect(page).toHaveURL('/tasks')

  await page.goto('/admin/services')
  await page.getByLabel('New service name').fill(serviceName)
  await page.getByRole('button', { name: 'Register service' }).click()

  const dialog = page.getByRole('dialog', { name: `Credentials for ${serviceName}` })
  await expect(dialog).toBeVisible()
  await expect(dialog).toContainText('API key')
  await expect(dialog).toContainText('API secret')
  await dialog.getByRole('button', { name: 'Close' }).click()

  await expect(page.getByRole('cell', { name: serviceName })).toBeVisible()

  // A brand new, credential-less browser context stands in for an anonymous
  // visitor: the public task list must not require the admin's session.
  const anonymousContext = await browser.newContext()
  const anonymousPage = await anonymousContext.newPage()
  await anonymousPage.goto('/tasks')
  await expect(anonymousPage.getByRole('heading', { name: 'Tasks' })).toBeVisible()
  await expect(anonymousPage.getByRole('link', { name: 'New task' })).toHaveCount(0)
  await anonymousContext.close()
})
