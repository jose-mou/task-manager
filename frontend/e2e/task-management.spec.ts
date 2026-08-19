import { expect, test } from '@playwright/test'

// Happy path over the real stack: create a task from the UI, see it listed,
// edit it and see the change reflected. Every other case is covered by the
// backend acceptance tests and the frontend unit tests.
test('creates, lists and edits a task', async ({ page }) => {
  const name = `E2E smoke ${Date.now()}`
  const updatedName = `${name} edited`

  await page.goto('/tasks')
  await page.getByRole('link', { name: 'New task' }).click()

  await page.getByLabel('Name', { exact: true }).fill(name)
  await page.getByLabel('Service', { exact: true }).fill('billing')
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
